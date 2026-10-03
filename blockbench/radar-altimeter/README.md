# Radar Altimeter — fonte visual

Identidade compartilhada: [guia de estilo e paleta](../../docs/visual-design-guide.md).

Abra `radar_altimeter.bbmodel` no Blockbench. Projeto Java Block/Item para
Minecraft 1.21.1, cuboides sem rotações, sete texturas pixeladas 32×32 incorporadas.
A base inicial foi preparada localmente; o conector principal foi dimensionado
e o detalhe central da antena foi pintado pela interface nativa do Blockbench.

## Direção de funcionamento

- A face de medição é **DOWN**, nunca a lateral dos conectores.
- A antena inferior ocupa `[3, 0, 3]` até `[13, 1, 13]`. Seu centro inferior é
  `(8, 0, 8)`, coincidente com a origem existente dos raycasts.
- As setas amarelas laterais apontam para a antena. Tampa e emblema azul ficam
  em cima; conectores são decorativos e não são portas configuráveis.
- Todas as peças permanecem entre 0 e 16, sem deslocamento para blocos vizinhos.
- Não há transparência, emissão, animação ou alteração da lógica de amostragem.

## Exportação

Salve o `.bbmodel` com as texturas incorporadas. Depois rode, na raiz do mod:

```powershell
node blockbench/radar-altimeter/export-assets.mjs
```

O exportador extrai os PNGs e converte UVs 32×32 para o espaço 0–16 do modelo
Minecraft, preservando as edições salvas. O modelo de item herda o de bloco.
Não execute `create-base-model.mjs` sobre a fonte refinada: ele é apenas o
bootstrap inicial, não o exportador.

Mudanças na geometria exigem atualizar a forma em `RadarAltimeterBlock`.
Os testes comparam o volume exportado e a colisão, além de verificar a face
inferior, referências de textura, UVs e limites do bloco.
