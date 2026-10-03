# Módulo de luz 1×1 — estudo visual

Identidade compartilhada: [guia de estilo e paleta](../../docs/visual-design-guide.md).

Modelo e pintura criados pela interface do Blockbench, no formato **Java Block/Item**, compatível com o alvo Minecraft 1.21.1. Sem texto ou comportamento novo.

- `lamp_module_1x1.bbmodel`: referência apagada, com texturas incorporadas.
- `lamp_module_1x1_on.bbmodel`: referência acesa, com texturas incorporadas. O material emissivo do Blockbench serve apenas para a prévia.
- `export/assets/aeronautics_fly_by_instruments/`: exportação dos modelos JSON e das texturas PNG; os modelos ativos do jogo ficam em `src/main/resources`.

## Dimensões e materiais

O conjunto ocupa **3,4 × 3,4 pixels**, com **2 pixels de altura** — próximo da largura e igual à altura do placeholder 1×1. As coordenadas X/Z estão centralizadas em 8. O Y=0 é a face de apoio no painel; o renderer a eleva até a superfície da mesa.

| Parte | Limites Y | Material |
| --- | --- | --- |
| `housing_opaque` | 0–0,5 | Base escura, inteiramente opaca nas faces mapeadas, inclusive a inferior. |
| `lens_tint` | 0,5–2 | Vidro vermelho translúcido uniforme (RGBA 212, 29, 56, 156). |
| `emitter_intensity_0_15` | 0,7–1,2 | Núcleo interno separado; referência escura apagada e clara acesa. |

São três cubos, sem meshes, com UV por face e texturas de 16×16. Não há cullface, pois o módulo deverá aparecer sobre a mesa, sem depender de blocos vizinhos. Os exports declaram renderização translúcida; base e núcleo têm texels opacos em suas áreas UV.

## Cores e intensidade futuras

A lente (`lamp_lens_red`) está separada da base e do núcleo: variantes de cor podem substituir sua textura sem mudar geometria ou UV. O núcleo também pode receber variações visuais independentes. Não há texto pintado.

As duas referências representam os extremos visuais **apagado (0)** e **aceso (15)**. O renderer do jogo reutiliza esses modelos: separa as faces por textura e aplica um passe emissivo ao núcleo e à lente de cada lâmpada ativa, mantendo a carcaça com iluminação normal. Um override temporário via CC:Tweaked permite avaliar intensidades visuais de 0 a 15 sem gravar sinais ou emitir luz dinâmica no mundo. O módulo 2×1 alongado reutiliza esses mesmos materiais; veja [a fonte Blockbench 2×1](../lamp-module-2x1/README.md) e [emissão visual e teste com CC:Tweaked](../../docs/lamp-emission.md).

## Reexportar e verificar

Para refazer o acabamento uniforme da lente nas duas fontes Blockbench e sincronizar a textura do jogo:

```powershell
node .\blockbench\lamp-module-1x1\make-lens-uniform.mjs
```

Depois de salvar outras alterações nos projetos pelo Blockbench, atualize os modelos JSON e as texturas exportadas:

```powershell
node .\blockbench\lamp-module-2x1\create-models.mjs
node .\blockbench\lamp-module-1x1\export-assets.mjs
```

O gerador 2×1 usa as fontes 1×1 para atualizar sua variante alongada e os dois modelos JSON ativos. O exportador então valida os cubos, a equivalência geométrica entre estados, os UVs e as texturas PNG incorporadas. Extrai as texturas sem repintá-las e não altera os projetos. Para a referência apagada, usa o primeiro projeto; para a acesa, usa apenas os materiais efetivamente atribuídos no segundo.

Validação visual das fontes realizada no Blockbench. A emissão do jogo vem do renderer, não do material de prévia do `.bbmodel`. Transparência, iluminação e ordenação entre módulos precisam de validação in-game após cada mudança de renderização.
