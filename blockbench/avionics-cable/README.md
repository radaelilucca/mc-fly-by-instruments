# Avionics Cable — fonte Blockbench

Fonte editável: `avionics_cable.bbmodel`, formato Java Block/Item, cubo Minecraft
de 16 unidades e quatro texturas próprias de 32×32 incorporadas. Refinado no
Blockbench; os PNGs ao lado são cópias de trabalho do editor.

O hub mantém seção de 4×4. A capa graphite tem nervuras discretas, o hub uma
marca azul de dados e o conector uma luva de latão. Sem transparência/emissão.

Grupos com nomes estáveis usados pelo exportador:

- `hub`: centro de 6 a 10 nos três eixos.
- `straight_arm`: braço normal de cabo/cabo ou endpoint sem encaixe adaptativo.
- `flexible_arm`: protótipo SOUTH, de Z=8 a Z=13; deformado no bake do chunk.
- `connector`: alívio de tensão, luva de 4,5×4,5 e ponta de 3,5×3,5; Z=13 a 16.

A ponta menor cabe nos sockets de 4×4 dos sensores. O encaixe termina exatamente
na superfície, sem profundidade adicional em faces que já chegam à borda.
O trecho flexível absorve o desvio até sockets fora do centro. A geometria fonte
não recebe compensações específicas para cada sensor.

Após salvar no Blockbench, executar na raiz do projeto:

```powershell
node blockbench/avionics-cable/export-assets.mjs
```

O exportador lê a fonte salva, preserva os refinamentos, gera as seis variantes
retas, os protótipos flexíveis, o item e as texturas em `src/main/resources`.
Não executar `create-base-model.mjs` sobre esta fonte: é apenas o bootstrap
original e substituiria refinamentos nativos.

As coordenadas de encaixe e os limites desta entrega estão em
[Encaixe visual dos cabos](../../docs/avionics-cable-visuals.md).
