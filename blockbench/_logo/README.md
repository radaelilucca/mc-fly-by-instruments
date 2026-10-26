# Cena da logo

`logo_scene.bbmodel` (Generic Model) reúne os três sensores para a arte da logo. Cada
sensor é um grupo raiz com pivô no centro da base: selecione o grupo e mova ou gire
livremente, sem afetar as fontes dos blocos.

| Grupo | Posição inicial | Giro (Y) |
| --- | --- | --- |
| `terrain_scanner_basic` | `[22, 0, -3]` | 30° |
| `radar_altimeter` | `[0, 4, 5]` | 45° |
| `terrain_scanner_advanced` | `[-22, 0, -3]` | -30° |

A frente dos scanners aponta para -Z (norte). Olhando por esse lado, +X fica à
esquerda.

A textura de latão do Create no topo do altímetro não vai embutida (licença). Ela é
carregada de `reference/create-textures/brass_casing.png`, fora do git.

Para regenerar a cena a partir dos modelos atuais (isso sobrescreve as posições):

```powershell
node blockbench/_logo/compose-logo-scene.mjs
```
