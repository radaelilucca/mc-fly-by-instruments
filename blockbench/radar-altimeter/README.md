# Radar Altimeter — fonte visual (v2, família de latão)

Guia obrigatório: [guia visual v2](../../docs/visual-design-guide.md). Referências diretas:
optical sensor (Simulated), content observer, stockpile/threshold switch e brass
casing (Create).

Abra `radar_altimeter.bbmodel` no Blockbench (Java Block/Item, 1.21.1, *Box UV* desligado).

## Peças

| Elemento | Volume | Material |
| --- | --- | --- |
| `body` | `[0,2,0]–[16,16,16]` | Laterais: faixa de latão, madeira escura entre colunas de latão, fenda de redstone. Topo: `create:block/brass_casing` (referência, sem cópia). Fundo: madeira escura com borda e rebites de latão |
| `lens_bezel` | `[2,1,2]–[14,2,14]` | Aro de latão de 2 px |
| `downward_antenna` | `[4,0,4]–[12,1,12]` | Lente de vidro escuro com anéis concêntricos. Centro inferior em `(8, 0, 8)` = origem dos raycasts |

Três cubos, coordenadas inteiras, sem rotação.

## Texturas (1 texel = 1 unidade)

- `radar_altimeter.png` (32×32): atlas com lateral 16×14, fundo 16×16, aro 12×12 e
  borda 12×1.
- `radar_altimeter_powered.png` (32×32): o mesmo atlas com a fenda de redstone acesa.
  Usado por `radar_altimeter_powered.json`, que só troca o atlas.
- `radar_altimeter_lens.png` (16×16): vidro 8×8 e borda 8×1.
- `create:block/brass_casing`: textura do Create, carregada do jar dele em runtime.

**Espaço de UV único.** O formato Java Block do Blockbench usa a resolução do projeto
(32) para todas as texturas e ignora o "UV size" de cada textura no export. Por isso,
as UVs das texturas de 16 px estão dobradas no `.bbmodel`. Ao editar no Blockbench,
mantenha a resolução do projeto em 32×32.

**Assets do Create.** São *All Rights Reserved*. O `.bbmodel` só referencia a
`brass_casing`, sem embuti-la. Para vê-la no Blockbench, copie
`assets/create/textures/block/brass_casing.png` do jar do Create para
`reference/create-textures/brass_casing.png`. A pasta está no `.gitignore`, e o
Blockbench a encontra pelo `relative_path`.

## Pintar no Blockbench

Paleta: `blockbench/_palette/create-aeronautics.gpl`. Fique dentro das rampas de latão,
madeira escura, redstone e lente. A fenda acesa e a apagada ocupam os mesmos pixels nas
duas variantes do atlas.

## Exportação

Salve o `.bbmodel` com as texturas incorporadas. Depois, na raiz do mod:

```powershell
node blockbench/radar-altimeter/export-assets.mjs
```

O exportador grava `models/block/radar_altimeter.json`,
`models/block/radar_altimeter_powered.json` e os PNGs do nosso namespace. Ele falha se
algum cubo tiver rotação ou coordenada fracionária, ou se uma face não tiver 1 texel por
unidade. O modelo de item herda o de bloco e define a exibição na GUI vista de baixo
(`rotation [-30, 225, 0]`), para a lente aparecer no inventário.

`create-base-model.mjs` é só o bootstrap da v2. Não o rode sobre uma fonte já refinada no
Blockbench.

Mudou a geometria? Atualize `SHAPE` e `cableSocket` em `RadarAltimeterBlock` e rode
`RadarAltimeterAssetsTest` e `CableConnectionTest`.

## Pendente

- A variante `powered` está pronta, mas o bloco ainda não tem estado `powered`. Ligá-la
  exige adicionar a propriedade, alternar o modelo no blockstate e ajustar o teste que
  exige propriedades vazias.
- As texturas antigas `radar_altimeter_*` foram removidas no redesign v2 dos Terrain
  Scanners.
