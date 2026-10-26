# Revisão das interações da mesa — validação

Data: 2026-10-08. Contrato: [Mesa modular](modular-table.md).

## Implementado

- Resolvedor compartilhado de alvo, âncora, item principal e footprint, usado por
  instalação, bloco, preview e scroll. Secundária não instala/remove/rotaciona.
- Contextos vazios, com item incompatível ou com stack antigo são rejeitados.
  Inserção e consumo ocorrem somente no servidor, após aceitar o footprint.
- Mão principal vazia abre o módulo ou o painel geral; Shift abre o painel geral
  sobre qualquer módulo. O atalho funciona mesmo com a secundária ocupada.
- Seleção das laterais dos módulos e remoção pelo footprint completo nas seis
  orientações. Bordas/traseira da base não são atalhos de interface.
- Devolução ao inventário sem o descarte implícito do criativo quando cheio;
  sobras caem no mundo. Teardown drena o conteúdo antes de devolver os itens.
- Pickup nativo do Create, quebra e perda do suporte preservam conteúdo.
- Lista do painel abre lâmpada, buzzer e FMS. Acesso de painel/FMS usa o mesmo
  alcance global de oito blocos, com transformação Sable e bloco carregado.
- Tokens temporários para mesa e instalação nos quatro payloads de telas/ações;
  protocolos de painel/FMS atualizados para versão 2. Comandos antigos não
  modificam uma instalação substituída. Tokens não são persistidos.
- Teste de som usa volume do rascunho sem salvá-lo. Save publica uma configuração
  de uma vez; Cancel descarta; fechamento comum aplica o rascunho válido uma vez.
- Formato persistido do grid/FMS e API Lua mantidos.

## Rodadas e evidências

A base anterior tinha 40 testes focados aprovados, sem exercitar a sequência
completa de `ServerPlayerGameMode.useItemOn`. O diagnóstico anterior foi por
leitura do código; não foi feita uma reprodução visual do cliente antigo nesta
entrega. Novos testes exercitam os callbacks reais do servidor.

### Testes focados: 57 aprovados

Incluem grid, persistência, peripheral, coordenadas, footprints nas seis
orientações, tokens após configuração/remoção/reload, teardown idempotente,
protocolos, FMS e ciclo de vida da tela do painel. Os testes de tela capturam o
payload enviado e não dependem de um cliente gráfico ou de um servidor remoto.

Log: `build/table-unit-tests-console.log`.

### GameTests: 16 aprovados

Executados em servidor NeoForge com Create, Aeronautics, Sable, CC:Tweaked e R-UI
locais, em `run-gametests`, separado do mundo `New World`.

- Último item em sobrevivência e cliques vazios repetidos.
- Principal vazia com módulo na secundária; contexto antigo após trocar o item.
- Sobreposição e footprint fora do grid sem consumo.
- Criativo e FMS nas seis orientações; células cobertas resolvem a âncora.
- Dois jogadores disputando a mesma célula: somente um coloca/consome.
- Painel geral, individual, Shift com secundária ocupada e abertura pela lista.
- Cliques laterais para abertura/remoção nas seis orientações; borda sem UI.
- Inventário criativo cheio: devolução por drop, sem perda nem duplicação.
- Preview de som sem alterar volume; comando antigo após reinstalar; mesa antiga.
- Abertura do FMS pelas seis células e pela lista; comandos de FMS antigos.
- Instalação com Shift; wrench secundária não remove nem recolhe.
- Perda do suporte devolve conteúdo uma vez.
- Pickup nativo com FMS preserva UUID, waypoints e destino.
- Outro item e colocação rejeitada não abrem UI.
- Mesa/FMS em **sublevel real do Sable**: abertura pela distância transformada;
  distância global acima de oito blocos rejeitada.
- Matriz adicional de **30 combinações**: cinco footprints nas seis orientações,
  colocação real em sobrevivência e remoção pela última célula coberta. Footprint
  do preview coincide com o instalado; FMS na secundária não interfere.

O jogador de teste usa `ServerPlayerGameMode` real, mas não realiza login nem
handshake de cliente: o listener captura os destinos dos payloads. Isso evita
eventos de login de mods externos que exigem um cliente negociado. Portanto os
testes provam roteamento e estado no servidor, não aparência/entrega visual.

Durante as rodadas foi encontrada e corrigida a omissão do atalho Shift quando
a secundária está ocupada: o bypass vanilla considera ambas as mãos. O hook de
interação permite especificamente a ação com principal vazia. Duas asserções de
teste foram corrigidas: comparação de `Optional<UUID>` e distância bruta (o método
vanilla de distância também é modificado pelo Sable). A matriz de remoção conta
os itens devolvidos no inventário principal, sem contar o FMS já na secundária.

Logs: `build/table-gametests-console.log`, `run-gametests/logs/latest.log`.

```powershell
.\gradlew.bat runGameTestServer -PtableGameTests --offline --no-daemon -x createMinecraftArtifacts
```

O harness só é compilado com `-PtableGameTests`; não integra o JAR normal.
O servidor encerra automaticamente ao finalizar os testes.

### Suíte geral e artefato

151 testes: 144 aprovados, as mesmas sete falhas pré-existentes:

1. `SensorOutputConfigScreenTest.advancedTabsKeepIndependentDraftRangesAndGhostTargets`
2. `SensorOutputConfigScreenTest.allSensorsShareInventoryWidthAndKeepActionsInsideThePanel`
3. `FmsAssetsTest.portugueseAndEnglishCoverEveryFmsVisibleString`
4. `AdvancedTerrainScannerAssetsTest.housingAndSocketGeometryRemainIdenticalToBasicInAllFourOrientations`
5. `RadarAltimeterPeripheralTest.sendsSnapshotOnStatusOrValueChangeButNotTimestampOnly`
6. `TerrainScannerAssetsTest.frontAntennaAndBlockstateRotationsAgreeWithCastDirection`
7. `TerrainScannerPeripheralTest.emitsFullScanOnContentChangeButNotTimestampOnly`

Nenhuma falha nova relacionada à mesa. Compilação, `assemble` e geração do JAR
aprovados; **o build completo não é verde**, devido à suíte geral acima.
Log: `build/table-full-validation-console.log`.

## Confirmação gráfica pendente

Havia um cliente aberto com o código anterior. Ele foi preservado: não foi
encerrado à força nem foi iniciado outro cliente concorrente. O mundo
`New World` não foi usado pelos GameTests. Não foi feita validação visual nesta
entrega; os testes de ciclo de vida não comprovam uma tecla ESC física ou o
raycast/preview efetivamente renderizado no cliente.

Após salvar e fechar normalmente o cliente, iniciar novamente pela task habitual:

- Confirmar grid/preview e scroll apenas da mão principal, inclusive com itens
  diferentes na secundária; scroll dentro das telas não deve girar módulos.
- Instalar o último módulo, esvaziar ambas as mãos e clicar em outras células.
- Abrir painel geral por célula vazia e por Shift sobre módulo/grid cheio.
- Abrir lâmpada/buzzer/FMS pelo mundo, pelas laterais e pela lista do painel.
- Remover módulos por células cobertas e verificar inventário/drop.
- No buzzer, mudar volume, testar som e Cancel; reabrir e verificar volume antigo.
  Repetir com Save e com ESC; conferir o beep e seu alcance real no avião.
- Repetir abertura e edição em mesa montada em avião/sublevel.
- Salvar/reabrir mundo, conferir sincronização e FMS removido/reinstalado.

Esses itens são o gate manual restante; testes e geração do JAR não substituem
a confirmação visual em um cliente reiniciado.
