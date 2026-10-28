# Wire Support — ferro industrial, braçadeira do Avionics Wire

Guia: [visual-design-guide.md](../../docs/visual-design-guide.md). Uso:
[avionics-wire.md](../../docs/avionics-wire.md). Referências: bearings do
Aeronautics (ferro industrial), wire hook do vídeo de referência
(`reference/visual/wires-video/`).

> **Estado:** base gerada pelo bootstrap em 2026-10-28 (refeita no mesmo dia:
> a sela plana não segurava o fio numa parede), ainda sem refino no Blockbench
> nem conferência no jogo.

## Identidade

- **Ferro industrial** (rampa `n o p q v W`), sem latão: o suporte é estrutura e
  não carrega dado.
- **Braçadeira**: o fio passa *por dentro*, entre a base e a tampa, como numa
  argola. Aberta nos quatro lados, serve para fio em qualquer direção no plano
  da face, sem estado de orientação, e segura o fio também em parede e teto.
- Tampa com quadrados concêntricos (vocabulário do guia), base e colunas mais
  escuras.

## Peças

Modelado com FACING = UP (bloco hospedeiro embaixo).

| Elemento | Volume | Função |
| --- | --- | --- |
| `base` | 4–12 × y 0–1 × 4–12 | chapa presa na face |
| `post_0..3` | 1×1 × y 1–3 nos cantos (4 e 11) | colunas |
| `cap` | 4–12 × y 3–4 × 4–12 | tampa que prende o fio |

6 cubos, textura própria 16×16 (`wire_support`). O centro do fio fica em
y = 2 (`WireAttachPoints.SUPPORT_WIRE`), meio texel de folga acima e abaixo.
Até três fios passam lado a lado, a 2 px um do outro, no vão de 6 px entre as
colunas. A forma do bloco é o volume externo (`WireSupportBlock.PARTS`).
`WireAssetsTest` confere o vão, as pistas e o volume.

## Pipeline

```powershell
node blockbench/wire-support/create-base-model.mjs   # só enquanto não houver refino
node blockbench/wire-support/export-assets.mjs
```

## Pendente

- Refino no Blockbench e conferência no jogo nas seis faces, com três fios.
- Fio em diagonal no plano da face: as pistas de fora podem raspar nas colunas.
