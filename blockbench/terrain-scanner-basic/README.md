# Terrain Scanner (Basic) — fonte visual (v2, tier de andesito)

Guia obrigatório: [guia visual v2](../../docs/visual-design-guide.md), seção
"Diferenciação entre os sensores". Referências diretas: andesite casing (Create), laser
sensor e laser pointer (Simulated).

Abra `terrain_scanner_basic.bbmodel` no Blockbench (Java Block/Item, 1.21.1, *Box UV*
desligado, resolução 32×32). A fonte aponta para **NORTH**: a frente fica em z = 0.

## Identidade

- **Material:** andesito e madeira média, o tier básico do Create (cinza e marrom
  claro). Não usa latão.
- **Óptica:** bocal escalonado, como um telescópio: anel de andesito médio, anel de
  andesito claro e lente escura pequena. O centro frontal `(8, 8, 0)` aponta para a
  origem dos raycasts.
- **Redstone:** fenda **vertical** nas duas laterais. O Basic emite o mesmo sinal em
  todas as faces.
- **Traseira:** socket de dados central, sem fenda.

## Peças

| Elemento | Volume | Material |
| --- | --- | --- |
| `body` | `[0,0,4]–[16,16,16]` | Moldura de andesito com tábuas verticais. Laterais com fenda, traseira com socket, frente com rebites |
| `nozzle_ring` | `[3,3,2]–[13,13,4]` | Andesito médio |
| `nozzle_tip` | `[4,4,1]–[12,12,2]` | Andesito claro |
| `forward_antenna` | `[5,5,0]–[11,11,1]` | Lente escura com brilho azul |

## Texturas (1 texel = 1 unidade)

- `terrain_scanner.png` (32×32): lateral 12×16, traseira 16×16, frente 16×16 e
  tampa 16×12 (a mesma região serve para cima e para baixo).
- `terrain_scanner_powered.png`: o mesmo atlas, com as fendas acesas.
- `terrain_scanner_optics.png` (32×32): anel, ponta e lente.

## Exportação

```powershell
node blockbench/terrain-scanner-basic/export-assets.mjs
```

O exportador gera `terrain_scanner.json`, `terrain_scanner_powered.json` e os PNGs.
Ele usa `blockbench/_tools/bbtools.mjs` e falha se houver rotação, coordenada
fracionária ou face fora de 1:1. `create-base-model.mjs` é só o bootstrap: não o rode
sobre uma fonte já refinada no Blockbench.

Mudou a geometria? Atualize `NORTH_SHAPE` em `TerrainScannerBlock`, os sockets em
`housingSocket` e rode `TerrainScannerAssetsTest`, `AdvancedTerrainScannerAssetsTest`
e `CableConnectionTest`.
