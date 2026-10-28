# Mesa modular — montagem e interações

A base mantém espessura de 3/16 de bloco, com moldura de latão e tampo de
madeira escura. Mesas vizinhas no mesmo plano e na mesma face formam um painel
contínuo: a moldura contorna o conjunto (texturas conectadas, veja a
[fonte visual da base](../blockbench/modular-table-base/README.md)).
Sua face exposta é dividida em 3 colunas × 3 linhas: cada célula tem
16/3 pixels de largura e profundidade. Ela pode ser montada em qualquer face
sólida, inclusive no teto do cockpit. As lâmpadas 1×1 e 2×1 têm modelos próprios
com carcaça opaca, vidro vermelho translúcido e emissão visual por módulo. A
mesa emite luz de bloco conforme a lâmpada mais forte acesa. Veja
[emissão e teste dos módulos](lamp-emission.md). Módulos novos são associados
ao `gpws.any`, com severidade mínima Caution. Ao clicar com a mão vazia em uma
lâmpada ou buzzer, abre a configuração individual; ao clicar numa célula vazia,
abre o painel geral. O setup básico aplica esse canal e severidade a todos os
annunciators.

O módulo de buzzer `buzzer` ocupa uma célula 1×1 e tem modelo cilíndrico preto,
ligeiramente menor que a lâmpada: lateral com acabamento preto piano e topo
poroso com pequeno furo central. O som acompanha a posição do módulo no mundo,
inclusive quando montado em avião/sublevel. Volume individual de 0 a 100%
controla também o alcance: até 16 blocos a 100%, reduzindo proporcionalmente.
Sua fonte
editável está em [Blockbench: buzzer 1×1](../blockbench/buzzer-module-1x1/README.md).

## Montagem

Todas as operações físicas, o preview e a rotação usam **somente a mão principal**.
A secundária não instala nem remove módulos, mesmo quando a principal está vazia.

| Mão principal / ação | Alvo | Resultado |
| --- | --- | --- |
| Vazia, clique direito | Célula vazia na face exposta | Painel geral |
| Vazia, clique direito | Lâmpada ou buzzer | Configuração individual |
| Vazia, clique direito | Qualquer célula coberta pelo FMS | Interface do mesmo FMS |
| Vazia, Shift + clique direito | Face exposta ou módulo | Painel geral, inclusive com grid cheio |
| Módulo, clique direito, com ou sem Shift | Footprint livre na face exposta | Instalar uma unidade |
| Módulo, clique direito | Sobreposição ou fora do grid | Rejeitar sem consumir nem abrir tela |
| Wrench, clique direito | Módulo instalado | Remover o módulo inteiro |
| Wrench, clique direito | Célula vazia | Nenhuma ação ou interface |
| Wrench, Shift + clique direito | Mesa | Recolher pela ação nativa do Create; devolver conteúdo uma vez |
| Outro item | Mesa | Não abrir interface; preservar uso do item |

O topo e as laterais do volume de seleção do módulo permitem abrir ou remover
o módulo inteiro. A instalação exige a face exposta. Traseira e bordas da base
não abrem interfaces. Um item devolvido que não cabe no inventário cai no mundo,
inclusive no criativo. Quebra, recolhimento e perda do suporte devolvem o conteúdo
sem duplicar. UUID, waypoints e destino do FMS acompanham seu item.

Nas configurações individuais, **Save** aplica o rascunho; **Cancel** descarta.
Fechar com ESC aplica o rascunho válido. **Test sound** usa o volume do rascunho
para um beep imediato, sem salvar esse volume. A lista do painel geral abre a
configuração correspondente, incluindo o FMS.

As mensagens das telas exigem mesa carregada, mesmo mundo e alcance global de
oito blocos (posição transformada no Sable). Tokens temporários distinguem a mesa
e cada instalação: uma tela antiga não modifica um módulo removido/reinstalado
nem uma mesa substituída. Não há alteração do formato persistido ou da API Lua.

1. Pegue a Base da Mesa Modular e os módulos de lâmpada ou buzzer na aba Aeronautics: Fly by Instruments.
2. Coloque a base sobre uma face sólida, incluindo a parte inferior de um bloco.
3. Segure um módulo na mão principal e mire na face exposta: o grid mostra seu footprint.
   Verde indica que ele cabe; vermelho indica colisão ou borda.
4. Com o módulo 2×1, role o scroll enquanto mira na mesa para alternar entre
   orientação horizontal (2 colunas × 1 linha) e vertical (1 coluna × 2 linhas).
5. Clique com o botão direito para instalar. Em sobrevivência, uma unidade do
   item é consumida; no criativo, o item é mantido.
6. Segure a Wrench do Create e clique com o botão direito em um módulo para
   removê-lo. O item retorna ao inventário ou cai se não houver espaço.

```mcfunction
/give @s aeronautics_fly_by_instruments:modular_table_base
/give @s aeronautics_fly_by_instruments:lamp_module
/give @s aeronautics_fly_by_instruments:lamp_module_2x1
/give @s aeronautics_fly_by_instruments:buzzer_module
```

O highlight mostra todo o footprint. É possível instalar até nove lâmpadas 1×1
ou combinar módulos que ocupem duas células. Não há sobreposição nem footprints
compartilhados entre mesas vizinhas. Módulos são devolvidos ao quebrar a mesa;
salvar/reabrir o mundo preserva sua ocupação.

## Regras de footprint

As coordenadas internas são zero-based. Um módulo é descrito por tipo, coluna
e linha de origem e footprint retangular (largura × altura). Todas as células
do footprint pertencem ao mesmo módulo. A inserção só ocorre se o retângulo
estiver inteiramente dentro do grid do painel (uma mesa sozinha ou várias unidas,
veja abaixo) e todas as células estiverem livres.
Uma operação de remoção sobre qualquer célula coberta retira o módulo inteiro.

O módulo `lamp` ocupa 1×1. O `lamp_2x1` ocupa 2×1 por padrão e alterna para 1×2
com o scroll; seu modelo é um tubo horizontal com as mesmas texturas da 1×1 e gira
junto com o footprint.
Ambos os footprints são persistidos e expostos pelas dimensões do módulo na API.
O `buzzer` ocupa sempre 1×1 e segue as mesmas regras de ocupação e remoção.
O `fms` ocupa 2 células de largura × 3 de altura (2×3), preenchendo toda a altura
da mesa e podendo começar em uma das duas colunas. Ele mantém UUID, waypoints e destino selecionado; ao remover
o módulo ou quebrar a mesa, tudo é gravado no item `fms_module` e acompanha o
módulo até sua próxima instalação.

Seu modelo tem carcaça grafite, aro prateado, tela preta nos dois terços
superiores e teclado compacto abaixo. A tela ainda é decorativa e opaca.
Fonte editável: [Blockbench FMS 2×3](../blockbench/fms-module-2x3/README.md).

## Mesas unidas (painel)

Mesas lado a lado no mesmo plano e montadas na mesma face (todas no chão, todas
na mesma parede ou no teto) formam um **painel**: um grid único com o total das
células das mesas unidas. Vizinhas só na diagonal não se unem. O limite é de 256
mesas por painel (`TablePanel.MAX_TABLES`).

- **Colocação:** a célula clicada é o canto de cima à esquerda do módulo, que pode
  atravessar a borda para a mesa seguinte (à direita ou abaixo no referencial do
  painel), por exemplo uma lâmpada 2×1 sobre duas mesas ou um FMS 2×3 sobre quatro.
  Todas as células precisam estar sobre mesas unidas e livres.
- **Interface:** clicar em qualquer mesa abre o mesmo painel geral, listando os
  módulos de todas as mesas com coordenadas do painel (contadas a partir da mesa de
  cima à esquerda). Teste, mudo e setup básico valem para o painel inteiro.
- **GPWS:** um cabo da rede aviônica em qualquer mesa alimenta os módulos de todas.
  A mesa líder do painel faz a leitura e distribui; o mudo cai junto em todas quando
  surge um alerta novo.
- **Armazenamento:** cada módulo continua salvo na mesa que tem a sua célula de
  origem. Unir ou separar mesas não move dados, e mundos antigos carregam como antes.
- **Quebra:** ao quebrar uma mesa, caem os módulos dela e os das mesas vizinhas que
  passavam por ela; os outros continuam no lugar. O painel restante se reorganiza
  sozinho. Se uma mesa sumir sem passar pela quebra normal (comando, por exemplo), a
  dona do módulo o derruba no tick seguinte. Mesas em chunk ainda não carregado
  nunca contam como ausentes.
- **CC:Tweaked:** cada mesa continua sendo um peripheral com o próprio grid 3×3
  (módulos ancorados nela). `setMuted`, `testPanel`, `applyBasicSetup` e
  `setTestSignal` agem sobre o painel inteiro.

## CC:Tweaked

Conecte um computador ou modem cabeado diretamente à mesa. O tipo de peripheral
é `modular_table`, exposto nas seis faces. A mesa também pode ser ligada à
Avionics Cable Network; `getNetworkSignals()` consulta canais publicados por
endpoints na rede física completa, sem carregar chunks. Os sinais de módulos
virtuais incluem `module_column` e `module_row` para distinguir módulos no mesmo
endpoint.

As coordenadas Lua são **1-based**, de 1 a 3. Colunas seguem o eixo horizontal
local da face e linhas seguem seu eixo vertical local. A origem permanece
consistente com o grid e a orientação da base.

| Método | Retorno |
| --- | --- |
| `getGridSize()` | `{columns = 3, rows = 3}` |
| `getModules()` | Lista dos módulos instalados |
| `getSlot(column, row)` | Estado da célula e, se ocupada, descrição do módulo |
| `canFit(column, row, width, height)` | Se o footprint cabe e está livre |
| `configureModule(column, row, channel, threshold)` | Configura uma lâmpada para sinais de teste locais (compatibilidade Lua) |
| `getAvailableChannels()` | Lista `gpws.any` e os sete canais do GPWS |
| `configureAlertModule(column, row, source, channel, minimumSeverity)` | Configura lâmpada ou buzzer; source é `gpws` ou `test`, severidade `advisory`, `caution` ou `warning` |
| `setBuzzerVolume(column, row, percent)` | Define volume e alcance do buzzer, de 0 a 100 |
| `applyBasicSetup()` | Associa todas as lâmpadas e buzzers ao alerta geral Caution; retorna quantos módulos configurou |
| `testPanel()` | Acende lâmpadas e exercita buzzers por 3 segundos |
| `setMuted(muted)` | Silencia/reativa os buzzers da mesa sem desligar lâmpadas |
| `getPanelStatus()` | Retorna estado da rede GPWS, mute e contagem de módulos ativos |
| `setTestSignal(channel, value)` | Define o valor simulado do canal |
| `getOutputs()` | Lista módulos, sinal recebido e estado ativo |
| `setLampTestIntensity(column, row, level)` | Sobreposição visual temporária 0–15 para uma lâmpada 1×1 ou 2×1 |
| `clearLampTestIntensity(column, row)` | Remove a sobreposição e restaura o estado do sinal/threshold |
| `getFmsWaypoints(column, row)` | Lista waypoints com UUID, nome, dimensão, coordenadas e seleção |
| `createWaypoint(column, row, name, x, y, z)` | Cria waypoint na dimensão atual |
| `updateWaypoint(column, row, id, name, x, y, z)` | Edita nome e coordenadas de um waypoint |
| `deleteWaypoint(column, row, id)` | Exclui waypoint e limpa o destino se necessário |
| `selectWaypoint(column, row, id)` / `clearWaypointSelection(column, row)` | Seleciona ou limpa o destino ativo |
| `getFmsSignals(column, row)` | Lê os canais do módulo indicado |
| `getNetworkSignals()` | Lê canais tipados de endpoints na rede de cabo conectada |

Módulos também incluem `source`, `alertChannel`, `minimumSeverity` e `buzzerVolume` na descrição e em `getOutputs()`. `signal` continua representando o sinal numérico local anterior; `severity` informa a severidade GPWS ativa. Módulo: `{occupied = true, type = "lamp", column = 1, row = 1, width = 1, height = 1, channel = "test", threshold = 0.5, source = "gpws", alertChannel = "gpws.any", minimumSeverity = 2, buzzerVolume = 100}`.
Uma célula livre retorna `{occupied = false, column = ..., row = ...}`.
Coordenadas ou dimensões fora de 1–3 geram erro Lua; um footprint válido que
ultrapasse a borda retorna `false`. `canFit` só consulta: não instala módulos.
Instalação e remoção são físicas. Métodos executam na thread principal.

Os métodos de intensidade funcionam com lâmpadas 1×1 e 2×1 e são apenas para teste visual: não alteram sinais,
thresholds ou dados persistidos da mesa. A luz de bloco da mesa acompanha o nível de teste.
O programa [lamp-emission-test.lua](examples/lamp-emission-test.lua) percorre
cada lâmpada instalada nos níveis 0 a 15, com meio segundo por nível.

O buzzer e o FMS também aparecem em `getModules()` e `getSlot()`. O FMS tem
footprint 2×3; seus métodos Lua validam que a célula inicial aponta para o FMS.
O canal `waypoint_selected` retorna uma leitura booleana `VALID`, verdadeira
quando existe um alvo escolhido. `distance_to_waypoint` retorna distância
horizontal X/Z em blocos; sem alvo ou em outra dimensão, seu estado é
`unavailable`. A distância usa a pose lógica/global do Sable sublevel. Eventos
`modular_table_fms_waypoint_changed` e `modular_table_fms_signal_changed`
notificam alterações confirmadas no servidor e atualizações do canal.

O `fms_navigator` é vinculado ao módulo exato ao selecioná-lo no inventário
pela aba Navigator da interface FMS; o item não é consumido. Vincular um item
que aponta para outro FMS exige marcar a confirmação de substituição. Na
Navigation Table do Aeronautics, ele segue o waypoint ativo somente no mesmo
mundo e sublevel do módulo associado. Sem FMS/target compatível, não fornece um
alvo; não carrega chunks. A API do FMS não injeta alertas no GPWS.

As coordenadas de waypoint são salvas em blocos inteiros. Valores fracionários,
incluindo os recebidos pela API Lua, são convertidos para o bloco que contém a
posição (floor, também para coordenadas negativas); waypoints fracionários de
saves antigos são normalizados ao serem carregados.

```lua
local tablePeripheral = assert(peripheral.find("modular_table"))
local waypoints = tablePeripheral.getFmsWaypoints(1, 1)
local signals = tablePeripheral.getFmsSignals(1, 1)
local connected = tablePeripheral.getNetworkSignals()
print(textutils.serialize(waypoints))
print(textutils.serialize(signals))
print(textutils.serialize(connected))
```

O `gpws.any` combina os alertas ativos; se mais de um GPWS estiver conectado,
prevalece a maior severidade por canal. `Advisory`, `Caution` e `Warning`
correspondem a 1, 2 e 3. Sem leitura utilizável ou sem GPWS conectado, os
módulos não ativam e a tela informa o estado da rede. Thresholds de voo ficam
no GPWS; o painel seleciona canal e severidade mínima.

Sinais de teste locais continuam disponíveis para scripts. Chamar
`configureModule` em uma lâmpada seleciona essa fonte, preservando os scripts
antigos que usam `setTestSignal`. Para buzzer, `configureAlertModule` seleciona
`source = "test"`; o canal e o limiar numérico podem ser configurados na tela.
Defaults novos e Basic setup usam `gpws.any` a partir de Caution.

Cada buzzer toca a partir da posição espacial do módulo. A posição da mesa é
transformada pela pose lógica do Sable; distância e ouvintes são calculados no
mundo global, para jogadores a bordo ou fora do avião. O mute cobre os alertas
atuais; alerta novo ou escalada reativa o som. Teste e mute não são persistidos.

Eventos novos: `modular_table_alert_outputs_changed` para atuação e
`modular_table_panel_state_changed` para mute/teste. Eventos existentes de
alterações, sinais de teste e FMS permanecem disponíveis.

Evento `modular_table_changed`: nome do peripheral, ação (`placed`, `removed`
ou `configured`) e descrição do módulo. Evento `modular_table_signal_changed`:
nome do peripheral, canal e valor. Só são emitidos após alterações confirmadas
no servidor; carregamento e sincronização não geram eventos.

```lua
local tablePeripheral = assert(peripheral.find("modular_table"), "Mesa não encontrada")
print(textutils.serialize(tablePeripheral.getGridSize()))
print(textutils.serialize(tablePeripheral.getModules()))
print("Cabe FMS 2x3:", tablePeripheral.canFit(1, 0, 2, 3))
tablePeripheral.configureModule(1, 1, "terrain", 30)
tablePeripheral.setTestSignal("terrain", 42)
print(textutils.serialize(tablePeripheral.getOutputs()))

while true do
    local _, name, action, module = os.pullEvent("modular_table_changed")
    print(name, action, module.type, module.column, module.row)
end
```

O sinal de teste é numérico e local à mesa: o módulo fica ativo quando
`signal >= threshold`. Valores e configuração persistem após salvar e reabrir o
mundo. Esses sinais não representam leituras da Avionics Cable Network nem
alertas GPWS. Os canais do FMS continuam publicados separadamente pelos
endpoints da mesa.

## Validação

Testes automatizados cobrem também roteamento por severidade, defaults, limites
de volume/alcance e compatibilidade de sinais locais. Ainda falta validar no
jogo o som espacial em voo/sublevels, instalação/remoção, conexão Lua real e
interação com múltiplos jogadores.
