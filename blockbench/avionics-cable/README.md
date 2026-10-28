# Avionics Cable v2.1 — ferro industrial com luva de latão

Guia: [visual-design-guide.md](../../docs/visual-design-guide.md). Referências
diretas: rope e belts do Create (peça linear discreta), fluid pipe (junção e
braços), bearings do Aeronautics (ferro industrial).

> **Estado:** modelo base gerado pelo bootstrap em 2026-10-27, ponto de partida
> para refino no Blockbench. Substitui a capa graphite/azul anterior (histórico
> do git).

## Identidade

- **Material:** capa em ferro industrial (rampa `n o p q v W`), discreta, para
  não competir com sensores e casings ao redor.
- **Latão:** só na luva do conector (`metal_ferrule`), 1 unidade de espessura.
  É o único ponto quente do cabo e marca onde ele encaixa no socket.
- **Conector:** bota de metal escuro, luva de latão e ponta de metal escuro, em
  degraus (motivo "bocal escalonado", seção 3 do guia).
- Sem redstone, sem gema, sem transparência ou emissão.

## Peças

Fonte modelada com o terminal em SOUTH (Z = 16 é o lado do dispositivo).

| Grupo | Elemento | Volume | Material |
| --- | --- | --- | --- |
| `hub` | `center_hub` | 6–10 nos três eixos | ferro industrial |
| `straight_arm` | `cable_to_cable` (oculto no editor) | 6–10 × 6–10 × Z 8–16 | ferro industrial |
| `flexible_arm` | `flexible_jacket` | 6–10 × 6–10 × Z 8–13 | ferro industrial |
| `connector` | `strain_relief` | 5–11 × 5–11 × Z 13–14 | metal escuro |
| `connector` | `metal_ferrule` | 5–11 × 5–11 × Z 14–15 | latão |
| `connector` | `socket_tip` | 6–10 × 6–10 × Z 15–16 | metal escuro |

Coordenadas inteiras, sem rotação, 6 cubos. Nomes de grupo e de cubo são
contrato com o exportador, `CableConnection` e `CableConnectionTest`.

Faces omitidas por encaixe (sem caps coplanares): sul do `flexible_jacket`, sul
da `strain_relief`, norte da `metal_ferrule`, norte do `socket_tip`. O sul do
`socket_tip` (face do plugue) existe na fonte, mas só vai para o modelo de item;
no bloco ele encosta no socket e o exportador o descarta.

## Texturas

Quatro PNG 16×16, 1 texel = 1 unidade, só cores da paleta (`PALETTE` do
`bbtools.mjs`):

| PNG | Regiões |
| --- | --- |
| `avionics_cable_jacket` | lateral 8×4 em (0,0); topo 4×8 em (8,0); base 4×8 em (12,0); ponta 4×4 em (0,4). Nervura a cada 4 unidades |
| `avionics_cable_hub` | lateral 4×4 em (0,0); topo em (4,0); base em (8,0) |
| `avionics_cable_relief` | face da bota 6×6 em (0,0); topo/base 6×1 em (6,0)/(6,1); laterais 1×6 em (12,0); ponta: topo/base 4×1 em (6,2)/(6,3), laterais 1×4 em (13,0), plugue 4×4 em (0,8) |
| `avionics_cable_ferrule` | face 6×6 em (0,0) (o miolo fica atrás da ponta); topo/base 6×1 em (6,0)/(6,1); laterais 1×6 em (12,0) |

O braço flexível usa os 5 primeiros texels das tiras da capa.

## Exportação

Após salvar no Blockbench, na raiz do projeto:

```powershell
node blockbench/avionics-cable/export-assets.mjs
```

Gera core, flex, terminal, os seis braços retos, o item e os PNGs em
`src/main/resources`. Falha com cubo rotacionado, coordenada fracionária ou face
fora de 1:1. Não rodar `create-base-model.mjs` sobre uma fonte refinada: ele é o
bootstrap e sobrescreve o refino.

## Mudou a geometria?

- `CableConnection.FLEX_START`/`FLEX_END` (Z 8 e 13) e `outline()` (caixas do
  conector).
- `AvionicsCableBlock` (`CORE` e `ARMS`).
- Rodar `CableConnectionTest`: ponta exatamente na superfície, outline envolvendo
  o modelo, texturas próprias opacas 16×16, coordenadas inteiras, faces de encaixe.

Encaixe nos sensores: [avionics-cable-visuals.md](../../docs/avionics-cable-visuals.md).

## Pendente

- Refino de pixel no Blockbench (nervura da capa, leitura do hub como junção).
- Validação no jogo: cabo ao lado de casings, nas traseiras dos sensores, em
  ramificações e no inventário. Só houve render local simplificado (three.js) e
  testes automatizados.
