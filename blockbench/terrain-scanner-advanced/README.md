# Advanced Terrain Scanner — fonte visual (v2, tier de latão)

Guia obrigatório: [guia visual v2](../../docs/visual-design-guide.md), seção
"Diferenciação entre os sensores". Referências diretas: content observer e smart
observer (Create), optical sensor (Simulated).

Abra `terrain_scanner_advanced.bbmodel` no Blockbench (Java Block/Item, 1.21.1, *Box UV*
desligado, resolução 32×32). A fonte aponta para **NORTH**: a frente fica em z = 0.

## Identidade

- **Material:** faixa de latão sobre corpo de **ferro industrial**, como o content
  observer (dourado sobre grafite). Distingue-se do Radar Altimeter, que é latão sobre
  madeira escura.
- **Óptica:** barra de latão panorâmica com **três lentes**, que espelham os canais de
  saída. A lente FRENTE `(8, 8, 0)` é a mais saliente. As lentes ESQUERDA (oeste,
  x baixo) e DIREITA (leste) ficam recuadas 1 unidade, separadas por divisórias de
  latão.
- **Redstone:** fendas **horizontais** só nas faces que emitem: laterais (ESQUERDA e
  DIREITA) e frente (abaixo das lentes).
- **Traseira:** socket central, sem fenda, porque não emite.
- **Tampa:** placa de latão com retângulos concêntricos.

## Peças

| Elemento | Volume | Material |
| --- | --- | --- |
| `body` | `[0,0,4]–[16,16,16]` | Mesmo volume do Basic (os sockets são idênticos) |
| `lens_bar` | `[0,4,2]–[16,12,4]` | Barra de latão com divisórias em x 4 e x 11 |
| `lens_left` | `[1,5,1]–[4,11,2]` | Lente do setor ESQUERDA |
| `lens_right` | `[12,5,1]–[15,11,2]` | Lente do setor DIREITA |
| `forward_antenna` | `[5,5,0]–[11,11,2]` | Lente do setor FRENTE, com linha de varredura azul |

## Texturas (1 texel = 1 unidade)

- `terrain_scanner_advanced.png` (32×32): lateral 12×16, traseira 16×16, frente
  16×16 e tampa 16×12.
- `terrain_scanner_advanced_powered.png`: fendas acesas. Uma única variante acende os
  três canais juntos; acender por canal exigiria três estados no bloco.
- `terrain_scanner_advanced_optics.png` (32×32): barra e três lentes.

## Exportação

```powershell
node blockbench/terrain-scanner-advanced/export-assets.mjs
```

Gera `terrain_scanner_advanced.json`, `terrain_scanner_advanced_powered.json` e os
PNGs, com as mesmas verificações do Basic. `create-base-model.mjs` é só o bootstrap.

Mudou a geometria? Atualize `NORTH_SHAPE` em `AdvancedTerrainScannerBlock` e rode
`AdvancedTerrainScannerAssetsTest` e `CableConnectionTest`.
