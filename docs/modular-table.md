# Mesa modular — grid e primeiro módulo

A base mantém espessura de 3/16 de bloco e textura placeholder de ferro.
Sua face exposta é dividida em 3 colunas × 3 linhas: cada célula tem
16/3 pixels de largura e profundidade. A lâmpada é um item montável 1×1,
representado temporariamente por uma miniatura de lâmpada de redstone apagada.
Ela ainda não emite luz nem reage a sinais ou alertas.

## Montagem

1. Pegue a Base da Mesa Modular e o Módulo de Lâmpada na aba Blocos Funcionais.
2. Coloque a base sobre um bloco, ou em outra face de suporte.
3. Segure a lâmpada e mire na face exposta: o grid aparece e a célula é destacada.
4. Clique com o botão direito para instalar. Em sobrevivência, uma unidade do
   item é consumida; no criativo, o item é mantido.
5. Para retirar, deixe as duas mãos vazias e use Shift + botão direito sobre
   a célula ocupada. O item retorna ao inventário ou cai se não houver espaço.

```mcfunction
/give @s flightsafetyinstruments:modular_table_base
/give @s flightsafetyinstruments:lamp_module
```

O highlight verde indica disponibilidade durante a instalação, e vermelho
indica ocupação. Durante a remoção, a célula selecionada aparece em verde.
É possível instalar até nove lâmpadas por mesa. Não há sobreposição nem
footprints compartilhados entre mesas vizinhas. Módulos são devolvidos ao
quebrar a mesa; salvar/reabrir o mundo preserva sua ocupação.

## Regras de footprint

As coordenadas internas são zero-based. Um módulo é descrito por tipo, coluna
e linha de origem e footprint retangular (largura × altura). Todas as células
do footprint pertencem ao mesmo módulo. A inserção só ocorre se o retângulo
estiver inteiramente dentro do grid e todas as células estiverem livres.
Uma operação de remoção sobre qualquer célula coberta retira o módulo inteiro.

As regras já suportam 1×2 e 2×2; os itens desses tamanhos serão adicionados
em etapas futuras. Só o módulo `lamp` de tamanho 1×1 pode ser montado ou
restaurado do save nesta versão.

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

Módulo: `{occupied = true, type = "lamp", column = 1, row = 1, width = 1, height = 1}`.
Uma célula livre retorna `{occupied = false, column = ..., row = ...}`.
Coordenadas ou dimensões fora de 1–3 geram erro Lua; um footprint válido que
ultrapasse a borda retorna `false`. `canFit` só consulta: não instala módulos.
Instalação e remoção são físicas. Métodos executam na thread principal.

Evento: `modular_table_changed`, seguido de nome do peripheral, ação
(`placed` ou `removed`) e descrição do módulo. Só é emitido após uma alteração
física confirmada no servidor; carregamento e sincronização não geram eventos.

```lua
local tablePeripheral = assert(peripheral.find("modular_table"), "Mesa não encontrada")
print(textutils.serialize(tablePeripheral.getGridSize()))
print(textutils.serialize(tablePeripheral.getModules()))
print("Cabe 2x2:", tablePeripheral.canFit(1, 1, 2, 2))

while true do
    local _, name, action, module = os.pullEvent("modular_table_changed")
    print(name, action, module.type, module.column, module.row)
end
```

## Validação

Testes automatizados cobrem ocupação, footprints maiores, bordas, mapeamento
de cliques nas seis orientações, persistência, sincronização por NBT e contrato
do peripheral/eventos. Ainda é necessário validar visualmente o highlight,
a montagem/remoção em jogo, a conexão Lua real e a interação em sublevels voadores.
