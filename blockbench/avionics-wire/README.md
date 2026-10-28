# Avionics Wire — fio preto e carretel

Guia: [visual-design-guide.md](../../docs/visual-design-guide.md), rampa
**Fio isolado** (`0 9 u`, seguida de `w` do metal escuro), aprovada em
2026-10-28 só para este fio.

O fio não é modelo de bloco: `WireRenderer` desenha um tubo de seção 1×1 texel
ao longo da linha central (caimento, curvas nos suportes). Por isso não há
`.bbmodel`; as duas texturas saem de mapas de caracteres em
`create-textures.mjs`.

| Arquivo | Uso |
| --- | --- |
| `textures/entity/avionics_wire.png` | Faixa 16×16. Cada face do tubo lê uma coluna (1, 5, 9, 13 = lado, baixo, outro lado, topo). O v corre ao longo do fio a 16 texels por bloco e repete, sem esticar. Uma nervura a cada 8 texels |
| `textures/item/avionics_wire.png` | Sprite do carretel: abas de latão, bobina preta, eixo de ferro industrial |
| `models/item/avionics_wire.json` | `item/generated` |

```powershell
node blockbench/avionics-wire/create-textures.mjs
```

## Pendente

- Gate visual no jogo: espessura de 1 texel (alternativa: 2). Caimento (1,5%
  do vão, máx. 0,3 bloco) e saída do conector (1 px reta, depois curva)
  ajustados em 2026-10-28 após o primeiro teste; conferir de novo.
