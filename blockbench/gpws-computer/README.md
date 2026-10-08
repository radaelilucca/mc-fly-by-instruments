# GPWS Computer

Gabinete robusto de aviónica: metal claro, painéis azul-petróleo, alça frontal,
botão de teste em latão, knob e três indicadores decorativos. Sem display,
fabricante real, translucência ou emissão. As funções existentes não mudam.

- Fonte Java Block/Item: `gpws_computer.bbmodel`, atlas opaco 64×64 incorporado.
- Frente canônica: NORTH (Z=0); origem de rotação `(8, 8, 8)`.
- Corpo principal: `(1, 1, 1)` até `(15, 15, 15)`; silhueta inteira dentro de 0–16.
- Sockets centrais na frente, em cima e embaixo chegam à borda da célula, mantendo o recuo
  da carcaça e a conexão visual com o cabo sem curvas ou offsets.
- Base recuada em Y=0,25: separa sua superfície inferior da face do socket.
- Refinamento nativo no Blockbench: alça com altura 1,25 e marca azul discreta.
- Colisão/seleção acompanham gabinete, controles e conectores; rotação horizontal.
- Transformações do item separadas da geometria do bloco, com ambas as mãos.

Após editar e salvar no Blockbench, execute na raiz do mod:

```powershell
node blockbench/gpws-computer/export-assets.mjs
```

`create-base-model.mjs` é apenas bootstrap e sobrescreve a fonte: não executá-lo
para exportar. O exportador preserva geometria e pintura do projeto salvo.

`preview.png` registra a primeira versão, anterior à remoção dos sockets
laterais e traseiro. A fonte e o JSON de runtime já usam apenas os três sockets.

Teste visual pendente no jogo: quatro orientações, cabo nas faces com socket,
inventário/mãos, colocação sobre chão e ausência de glitches com/sem shader.
