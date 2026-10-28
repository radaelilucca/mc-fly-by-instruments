# Módulo FMS 2×3 — fonte visual (v2.1, ferro industrial + latão)

Guia obrigatório: [guia visual v2.1](../../docs/visual-design-guide.md), seção 9.
Referências: navigation table do Simulated, nixie tube e display link do Create.

Abra `fms_module_2x3.bbmodel` no Blockbench (Java Block/Item, 1.21.1, *Box UV* desligado,
resolução 32×32). Modelado com a mesa voltada para cima: apoio em Y = 0, centro em X/Z = 8.
Z baixo é o topo do painel (longe do piloto): a tela fica em cima e os controles embaixo.

![FMS v2.1: vistas do piloto e de cima](preview.png)

## Identidade

Um console simples em vez da réplica de um FMS real: carcaça de ferro industrial, uma tela
de lente escura numa moldura de latão, dois knobs de latão e duas teclas. O latão fica só na
moldura e nos knobs. Sem teclado, sem teclas de seleção de linha, sem texto.

**Relevo de 1 unidade:** todo detalhe (tela, knobs, teclas) sobe exatamente 1 acima da
carcaça. A tela e a moldura são um único cubo; a moldura é pintada no topo dele.

A tela é uma superfície opaca, sem conteúdo nem emissão, reservada para uma implementação
posterior.

## Peças

| Elemento | Volume | Material |
| --- | --- | --- |
| `housing` | `[3,0,1]–[13,2,15]` | `create:block/industrial_iron_block` (recortes 1:1) |
| `screen` | `[4,2,2]–[12,3,10]` | topo 8×8: moldura de latão de 1 px + lente escura 6×6; laterais de latão |
| `knob_left`, `knob_right` | `[4,2,12]–[6,3,14]`, `[10,2,12]–[12,3,14]` | latão, marca de ponteiro no topo |
| `key_upper`, `key_lower` | `[7,2,11]–[9,3,12]`, `[7,2,13]–[9,3,14]` | tampo de ferro industrial claro |

Seis cubos, coordenadas inteiras, sem rotação. Carcaça de 10×14, altura máxima 3 (guia, 9.2).
Faces escondidas (fundo dos detalhes) ficam no arquivo como desativadas (`texture: null`): o
Blockbench 5.2 não abre um `.bbmodel` com face faltando.

## Texturas

- `fms_module.png` (32×32): topo da tela com a moldura, laterais de latão, knobs e teclas. Regiões em `UV` no `create-base-model.mjs`.
- `create:block/industrial_iron_block`: referenciada pelo id, nunca copiada. Para vê-la no
  Blockbench, extraia o PNG do jar do Create para `reference/create-textures/` (fora do git).
  Também é a partícula.

## Exportação

```powershell
node blockbench/fms-module-2x3/export-assets.mjs
```

Gera `models/block/fms_module_2x3.json` e `textures/block/fms_module.png`. Falha com rotação,
coordenada fracionária ou face fora de 1:1. `create-base-model.mjs` é só o bootstrap: não o
rode sobre uma fonte já refinada no Blockbench.

O modelo de item (`models/item/fms_module.json`) herda o de bloco. Mudou a geometria? Rode
`FmsAssetsTest`. O módulo não tem colisão própria (fica na mesa).

## Pendente

- Refino de pixels no Blockbench: a fonte atual é a do bootstrap. Conferido no Blockbench web
  5.2.2: abre com os 6 cubos e `Codecs.java_block.compile()` bate com o export.
- Validação no jogo: mesa no chão, parede e teto, saves legados 2×2 (o renderer achata a
  profundidade), transformações de item e shaders. `preview.png` é um render local
  simplificado (sem AO nem iluminação do jogo).
- Conteúdo da tela.
