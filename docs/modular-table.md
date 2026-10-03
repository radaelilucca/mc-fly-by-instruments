# Mesa modular — grid e primeiro módulo

A base mantém espessura de 3/16 de bloco e textura placeholder de ferro.
Sua face exposta é dividida em 3 colunas × 3 linhas: cada célula tem
16/3 pixels de largura e profundidade. Ela pode ser montada em qualquer face
sólida, inclusive no teto do cockpit. As lâmpadas 1×1 e 2×1 têm modelos próprios
com carcaça opaca, vidro vermelho translúcido e emissão visual por módulo. Veja
[emissão e teste dos módulos](lamp-emission.md). Na Fase 1, ambas servem como
atuadores de teste: acendem quando o sinal simulado do canal associado alcança
o threshold do módulo.

O módulo de buzzer `buzzer` ocupa uma célula 1×1 e tem modelo cilíndrico preto,
ligeiramente menor que a lâmpada: lateral com acabamento preto piano e topo
poroso com pequeno furo central. Nesta entrega ele pode ser instalado, removido
e salvo com a mesa; ainda não tem som nem configuração de sinais. Sua fonte
editável está em [Blockbench: buzzer 1×1](../blockbench/buzzer-module-1x1/README.md).

## Montagem

1. Pegue a Base da Mesa Modular e os módulos de lâmpada ou buzzer na aba Aeronautics: Fly by Instruments.
2. Coloque a base sobre uma face sólida, incluindo a parte inferior de um bloco.
3. Segure um módulo e mire na face exposta: o grid mostra seu footprint.
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
estiver inteiramente dentro do grid e todas as células estiverem livres.
Uma operação de remoção sobre qualquer célula coberta retira o módulo inteiro.

O módulo `lamp` ocupa 1×1. O `lamp_2x1` ocupa 2×1 por padrão e alterna para 1×2
com o scroll; a variante alonga a mesma geometria e gira junto com o footprint.
Ambos os footprints são persistidos e expostos pelas dimensões do módulo na API.
O `buzzer` ocupa sempre 1×1 e segue as mesmas regras de ocupação e remoção.

## CC:Tweaked

Conecte um computador ou modem cabeado diretamente à mesa. O tipo de peripheral
é `modular_table`, exposto nas seis faces. A rede Avionics Cable será uma etapa
posterior. Os módulos instalados são expostos por meio da mesa.

As coordenadas Lua são **1-based**, de 1 a 3. Colunas seguem o eixo horizontal
local da face e linhas seguem seu eixo vertical local. A origem permanece
consistente com o grid e a orientação da base.

| Método | Retorno |
| --- | --- |
| `getGridSize()` | `{columns = 3, rows = 3}` |
| `getModules()` | Lista dos módulos instalados |
| `getSlot(column, row)` | Estado da célula e, se ocupada, descrição do módulo |
| `canFit(column, row, width, height)` | Se o footprint cabe e está livre |
| `configureModule(column, row, channel, threshold)` | Associa qualquer módulo de lâmpada a canal e threshold |
| `setTestSignal(channel, value)` | Define o valor simulado do canal |
| `getOutputs()` | Lista módulos, sinal recebido e estado ativo |
| `setLampTestIntensity(column, row, level)` | Sobreposição visual temporária 0–15 para uma lâmpada 1×1 ou 2×1 |
| `clearLampTestIntensity(column, row)` | Remove a sobreposição e restaura o estado do sinal/threshold |

Módulo: `{occupied = true, type = "lamp", column = 1, row = 1, width = 1, height = 1, channel = "test", threshold = 0.5}`.
Uma célula livre retorna `{occupied = false, column = ..., row = ...}`.
Coordenadas ou dimensões fora de 1–3 geram erro Lua; um footprint válido que
ultrapasse a borda retorna `false`. `canFit` só consulta: não instala módulos.
Instalação e remoção são físicas. Métodos executam na thread principal.

Os métodos de intensidade funcionam com lâmpadas 1×1 e 2×1 e são apenas para teste visual: não alteram sinais,
thresholds ou dados persistidos da mesa e não emitem luz dinâmica no mundo.
O programa [lamp-emission-test.lua](examples/lamp-emission-test.lua) percorre
cada lâmpada instalada nos níveis 0 a 15, com meio segundo por nível.

O buzzer também aparece em `getModules()`, `getSlot()` e no evento
`modular_table_changed`, com `type = "buzzer"`, largura e altura 1. Em
`getOutputs()`, permanece inativo. Métodos de configuração e intensidade
de lâmpadas rejeitam esse módulo enquanto seu comportamento não for implementado.

Evento `modular_table_changed`: nome do peripheral, ação (`placed`, `removed`
ou `configured`) e descrição do módulo. Evento `modular_table_signal_changed`:
nome do peripheral, canal e valor. Só são emitidos após alterações confirmadas
no servidor; carregamento e sincronização não geram eventos.

```lua
local tablePeripheral = assert(peripheral.find("modular_table"), "Mesa não encontrada")
print(textutils.serialize(tablePeripheral.getGridSize()))
print(textutils.serialize(tablePeripheral.getModules()))
print("Cabe 2x2:", tablePeripheral.canFit(1, 1, 2, 2))
tablePeripheral.configureModule(1, 1, "terrain", 30)
tablePeripheral.setTestSignal("terrain", 42)
print(textutils.serialize(tablePeripheral.getOutputs()))

while true do
    local _, name, action, module = os.pullEvent("modular_table_changed")
    print(name, action, module.type, module.column, module.row)
end
```

O sinal é numérico e local à mesa: a lâmpada fica ativa quando
`signal >= threshold`. Valores de teste e configuração persistem após salvar e
reabrir o mundo. Esse canal não representa a futura Avionics Cable Network nem
alertas GPWS. O módulo 2×1 continua apenas estrutural durante essa fase.

## Validação

Testes automatizados cobrem ocupação, footprints maiores, bordas, mapeamento
de cliques nas seis orientações, persistência, sincronização por NBT e contrato
do peripheral/eventos. Ainda é necessário validar visualmente o highlight,
a montagem/remoção em jogo, a conexão Lua real e a interação em sublevels voadores.
