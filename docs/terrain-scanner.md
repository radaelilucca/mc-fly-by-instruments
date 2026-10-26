# Terrain Scanner

## Modelo visual Basic

Tier de andesito do Create: corpo de andesito com tábuas de madeira média e um
**bocal escalonado** na frente (anel de andesito médio, anel claro e lente escura
pequena), como um telescópio. As fendas de redstone verticais nas laterais acendem com
o sinal. A traseira tem o socket de dados central. Não há setas pintadas: o bocal mostra
a direção da varredura.

A fonte canônica aponta para NORTH. O bloco pode ser colocado de ponta-cabeça na face
inferior de um bloco: a carcaça e a colisão são invertidas, mas a frente e os casts
continuam apontando horizontalmente conforme `FACING`. O centro frontal da lente
`(8, 8, 0)` coincide com a direção dos casts. O modelo não emite luz.

Fonte e exportação: [Blockbench Basic](../blockbench/terrain-scanner-basic/README.md).

## Modelo visual Advanced

Tier de latão: faixa de latão sobre corpo de ferro industrial, tampa de latão com
retângulos concêntricos e uma **barra panorâmica com três lentes**. A lente FRENTE fica
saliente no centro; ESQUERDA e DIREITA ficam recuadas nas laterais, separadas por
divisórias de latão, e espelham os três canais de saída. As fendas horizontais aparecem
só nas faces que emitem (laterais e frente); a traseira, que não emite, tem apenas o
socket.

O corpo e os sockets têm o mesmo volume do Basic; a frente e a colisão são próprias.
O Advanced se distingue do Basic pelo material e pela óptica, e do Radar Altimeter pela
face de medição frontal e pelo corpo de ferro. A distinção está descrita no
[guia visual](visual-design-guide.md).

Fonte e exportação: [Blockbench Advanced](../blockbench/terrain-scanner-advanced/README.md).

## Funcionamento

O mod oferece duas variantes físicas de scanner. Ambas apontam horizontalmente conforme a orientação do bloco, inclusive quando montadas de ponta-cabeça no teto; acompanham a transformação do sublevel e detectam formas de colisão de blocos/contraptions. Entidades são ignoradas e o sublevel que contém o scanner é excluído. Scans não carregam chunks; fronteiras descarregadas reduzem a cobertura.

## Variantes

- `terrain_scanner`: nove raios fixos em cone de abertura total de 90°.
- `terrain_scanner_advanced`: distribuição aproximadamente uniforme de 9–81 raios. A abertura total é configurável de 30° a 180°, padrão 90°; a densidade é calculada automaticamente pela área angular.

Os dois scanners abrem a configuração de saída ao interagir. A tela tem a
largura do inventário, um slider de dois marcadores para delimitar a faixa em
blocos, frequências fantasma lado a lado e inventário/hotbar com seleção sem
consumir itens. O JEI, quando instalado, permite arrastar itens para os slots.
Tooltips explicam que a saída cresce com a proximidade: 15 no limite inferior
ou abaixo e 0 no limite superior ou acima; sem contato utilizável, é 0.

No Advanced, um slider ajusta a abertura total de 30° a 180°. Abas **Frente / Esq. /
Dir.** mantêm faixas e frequências independentes; as laterais só atuam acima de
50°. Alterações ficam em rascunho e são validadas no servidor ao aplicar.
**Salvar** envia e fecha, **Cancelar** descarta, e **ESC** aplica o rascunho
válido e fecha sem reabrir a tela.

O modo de saída é automático, tanto pela tela quanto por CC:Tweaked: sem par
completo, a saída usa as faces; qualquer par completo seleciona Redstone Link.
O modo continua global no Advanced: em Link, setores sem par completo ficam
desabilitados e não emitem pelas faces. Limpar todos os pares retorna à saída
local. `setOutputMode` permanece como compatibilidade e valida o nome, mas não
sobrescreve essa seleção automática.

O alcance máximo é controlado por `terrainScannerMaxRange` (padrão 128 blocos; limites de configuração 1–4096). A cadência usa `sensorScanIntervalTicks` (padrão 5 ticks), compartilhada com os demais sensores. A resolução do avançado é determinada automaticamente pela fórmula de cobertura angular; não há opção para configurar a contagem diretamente.

## Peripheral CC:Tweaked

Os blocos são endpoints da Avionics Cable Network e também disponibilizam peripherals próprios:

- `terrain_scanner`: `getScan()` e evento `terrain_scanner_scan_changed`.
- `terrain_scanner_advanced`: os mesmos métodos/evento, mais `getConfiguration()`, `setOpeningAngle(degrees)` e evento `terrain_scanner_configuration_changed`.

`getScan()` retorna um snapshot com:

| Campo | Significado |
| --- | --- |
| `status` | `valid`, `unavailable` ou `failed` |
| `contacts` | Um item por raio que atingiu uma forma de colisão; lista vazia também é uma leitura válida quando há cobertura completa sem obstáculos. |
| `contacts[i].rayIndex` | Índice do raio, começando em 1. |
| `contacts[i].distance` | Distância até o hit, em blocos. |
| `contacts[i].directionX/Y/Z` | Direção relativa ao scanner: direita, cima e frente, respectivamente. |
| `sampledRays` / `totalRays` | Raios concluídos e número total de raios na amostra. |
| `coverage` | Fração concluída entre 0 e 1. |
| `unit` | Sempre `blocks`. |
| `gameTime` | Tick do mundo em que a amostra foi calculada. |

Se ao menos um raio completar, o status é `valid`, mesmo com cobertura parcial; contatos conhecidos e a fração de cobertura são preservados. Se nenhum raio completar, o status é `unavailable`. Falhas inesperadas produzem `failed`. Não há classificação do tipo do hit: terreno, construções e outras contraptions aparecem como contatos equivalentes.

```lua
local scanner = peripheral.find("terrain_scanner")
assert(scanner, "Terrain Scanner não encontrado")

local scan = scanner.getScan()
print(scan.status, scan.sampledRays .. "/" .. scan.totalRays, scan.coverage)
for _, contact in ipairs(scan.contacts) do
  print(contact.rayIndex, contact.distance,
    contact.directionX, contact.directionY, contact.directionZ)
end

while true do
  local _, side, updated = os.pullEvent("terrain_scanner_scan_changed")
  print("scan atualizado em", side, updated.status, updated.coverage)
end
```

Configuração da variante avançada:

```lua
local scanner = peripheral.find("terrain_scanner_advanced")
assert(scanner, "Advanced Terrain Scanner não encontrado")

local config = scanner.getConfiguration()
print("Abertura total:", config.openingAngle)
assert(scanner.setOpeningAngle(120)) -- faixa aceita: 30–180 graus

while true do
  local _, side, updated = os.pullEvent("terrain_scanner_configuration_changed")
  print("nova abertura em", side, updated.openingAngle)
end
```

`setOpeningAngle` retorna `true` somente quando uma nova abertura válida foi aplicada. Chamar com o valor atual ou fora da faixa retorna `false`. Os eventos incluem o nome do peripheral como primeiro argumento após o nome do evento e o snapshot/configuração atualizados como argumento seguinte.

Os modelos Basic e Advanced têm assets próprios. As leituras também são
consumidas pelo motor do GPWS: contatos conhecidos podem acionar os canais
`TERRAIN` e `PULL_UP`; ausência de contatos com cobertura incompleta permanece
indisponível, não sendo interpretada como caminho livre. A validação integrada
em cenários de voo continua pendente para a estabilização.

## Visualização de raycasts (ambiente de desenvolvimento)

No cliente de desenvolvimento, pressione **F7** para alternar a visualização dos raycasts de Radar Altimeters e Terrain Scanners próximos (raio de 96 blocos). As linhas são atualizadas na mesma cadência configurada para os scans: vermelho indica hit, ciano indica caminho livre e âmbar indica que o raio parou antes de uma fronteira de chunk descarregada. O altímetro mostra seus nove raios descendentes; os Terrain Scanners mostram os raios do cone frontal. A visualização considera scanners em sublevels e não solicita carregamento de chunks. O keybind e o renderizador não são registrados em builds de produção.
