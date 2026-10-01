# Product Requirements Document — Aeronautics: Fly by Instruments

## 1. Visão geral

**Aeronautics: Fly by Instruments** será um addon de segurança e instrumentação de voo para Create Aeronautics. O mod ajudará jogadores a detectar riscos durante o voo e construir sistemas de alerta físicos dentro das próprias contraptions.

O produto não será inicialmente um pacote geral de aviação. Seu foco será:

- consciência de terreno;
- avisos de proximidade e trajetória;
- instrumentação conectada fisicamente;
- painéis de alerta modulares;
- integração programática completa com CC:Tweaked.

Este PRD consolida as decisões de discovery e define um roadmap macro. O documento de discovery continua sendo a referência para decisões conceituais detalhadas ainda em aberto.

## 2. Objetivos do produto

### Objetivos

1. Adicionar um sistema GPWS funcional para contraptions voadoras.
2. Permitir que o jogador construa uma rede física de sensores usando cabos.
3. Oferecer uma experiência simples por padrão e uma configuração avançada opcional.
4. Transformar dados de voo em alertas físicos e programáveis.
5. Expor cada componente por sua API completa para CC:Tweaked no mesmo momento em que a funcionalidade for implementada.
6. Manter o custo de processamento previsível em servidores multiplayer.

### Fora do objetivo inicial

- motores ou sistemas de propulsão;
- autopilot completo;
- substituição do `Altitude Sensor` original;
- radar de tráfego ou armamento;
- logística de rotas;
- HUD genérico sem relação direta com segurança de voo.

### Princípios de desenvolvimento

- **Mesa modular primeiro:** a fundação funcional do Annunciator Panel é a
  primeira entrega de gameplay após a fundação técnica do projeto. Ela deve ser
  testável antes de o GPWS, os scanners e os alertas finais existirem.
- **Integração CC vertical:** CC:Tweaked não será uma fase posterior. Cada
  funcionalidade deverá nascer com seus periféricos, métodos, eventos,
  documentação e testes de API correspondentes.

## 3. Público e experiência desejada

O mod deve atender dois perfis:

### Jogador casual

Deve ser possível instalar um GPWS com configuração mínima:

1. colocar o GPWS;
2. posicionar os sensores;
3. conectar os sensores com Avionics Cable;
4. montar um Annunciator Panel;
5. usar thresholds automáticos.

O jogador não deverá precisar conhecer a lógica real de um GPWS para obter alertas úteis.

### Jogador avançado

Deve ser possível configurar thresholds, histerese, regras de arming/inhibit, sensores preferenciais, redundância, fases de voo e roteamento de canais.

## 4. Escopo funcional inicial

### 4.1 GPWS Computer

O GPWS Computer será o componente lógico central. Ele interpretará dados da rede de avionics e publicará os estados de alerta.

Modos e contextos iniciais:

- `OFF`/`STANDBY`;
- `ARMED`;
- `TEST`;
- decolagem;
- subida;
- cruzeiro;
- descida;
- aproximação/pouso.

No modo básico, a seleção de fase será automática. No modo avançado, poderá ser ajustada ou substituída manualmente.

Alertas iniciais:

- `TERRAIN`;
- `PULL UP`;
- `SINK RATE`;
- `DON'T SINK`;
- `TOO LOW`;
- `BANK ANGLE`;
- `SENSOR FAIL`.

Thresholds, tempos de confirmação, histerese e prioridade final de cada alerta serão definidos antes da implementação do motor de alertas.

### 4.2 Radar Altimeter

O Radar Altimeter medirá a distância entre a aeronave e a superfície diretamente abaixo dela, em AGL.

Características esperadas:

- bloco físico orientado para baixo;
- leitura relativa ao terreno, não à altitude global;
- integração com o GPWS e CC:Tweaked;
- tratamento definido para ausência de superfície, chunks não carregados e alvos inválidos.

### 4.3 Terrain Scanner

O Terrain Scanner será um bloco físico, normalmente colocado no nariz da aeronave.

Haverá duas versões funcionais:

- **Scanner simples:** um raycast frontal único;
- **Scanner avançado:** varredura em hemisfério frontal com abertura e resolução configuráveis.

O scanner detectará como possíveis obstáculos:

- terreno;
- estruturas construídas;
- outras contraptions.

O alcance máximo será limitado pelo arquivo de configuração do mod. O scanner deverá ignorar a própria contraption ao interpretar os resultados.

### 4.4 Integração com sensores existentes

O mod usará os sensores existentes do Aeronautics como fontes de dados:

- `Velocity Sensor`;
- `Gimbal Sensor`;
- `Altitude Sensor`, quando necessário para altitude global e velocidade vertical.

O mod não recriará esses sensores no escopo inicial.

### 4.5 Avionics Cable

O Avionics Cable será o único componente novo de conexão física na primeira versão.

Requisitos:

- conexão automática com sensores compatíveis, GPWS e módulos;
- rede formada por cabos adjacentes e ramificações naturais;
- descoberta automática dos componentes alcançáveis;
- desconexão detectável como sensor ausente ou falha;
- transporte apenas de dados;
- nenhum sistema wireless como mecanismo principal;
- sem blocos obrigatórios separados de junction, data port ou frequency link.

O cabo acompanhará a contraption dentro do sublevel. A topologia física deverá ser significativa para a construção, sem exigir configuração manual de frequências.

### 4.6 Annunciator Panel

O Annunciator Panel será uma mesa modular inspirada nas mesas do Aeroworks.

Durante o design e a construção desse painel, o projeto deverá consultar
`reference/aeroworks-1.5.0.jar` como referência local para a experiência de
mesa modular, slots predefinidos, seleção de módulos e highlight visual. Esse
JAR é apenas material de referência; não é dependência de runtime ou compilação
do Aeronautics: Fly by Instruments, e nenhuma implementação ou asset deverá ser
copiado sem revisão de licença e necessidade explícita.

Requisitos:

- **Primeiro vertical slice:** criar as bases da mesa modular, o sistema de
  slots, a colocação e remoção de módulos, a persistência da configuração e o
  highlight visual do slot em edição antes da integração com os alertas finais.
- Deve existir pelo menos um item/módulo de teste que possa ser criado pelo
  jogador e usado para validar a ocupação, edição e leitura dos slots.
- A mesa e o módulo de lâmpada 1×1 devem funcionar com canal e saída simulados,
  configuráveis via CC:Tweaked, sem depender do GPWS ou dos sensores finais.
- slots predefinidos;
- highlight visual do slot em edição;
- módulos substituíveis, inicialmente incluindo lâmpadas e buzzers;
- configuração de cada módulo para um canal do GPWS;
- thresholds individuais de atuação;
- acesso aos estados via CC:Tweaked desde a primeira versão da mesa;
- eventos e métodos de API para ocupação, seleção, configuração e estado dos
  módulos aplicáveis.

Os módulos finais, acknowledge/mute, sons e integração com goggles/displays
serão definidos durante o design detalhado. A lâmpada 1×1 da Fase 1 é um
atuador de teste por canal simulado; não representa ainda o módulo final de
alerta GPWS.

## 5. API CC:Tweaked e integração vertical

Todos os sensores, computadores, cabos relevantes e módulos deverão nascer com
API completa para **CC:Tweaked 1.120.0+**, junto com a própria funcionalidade.
Não haverá uma implementação provisória sem CC seguida de uma fase posterior de
integração.

A API deverá permitir, no mínimo:

- enumerar sensores conectados;
- consultar leituras atuais;
- consultar a saúde/conectividade dos sensores;
- consultar modos e fase de voo do GPWS;
- consultar estado, severidade e origem dos alertas;
- configurar thresholds permitidos;
- consultar e configurar módulos do Annunciator Panel;
- observar eventos de mudança de alerta;
- distinguir dados indisponíveis de leituras válidas.

A integração CC:Tweaked será requisito de primeira versão, não um recurso posterior.

Cada fase deverá entregar, quando aplicável:

- o peripheral correspondente;
- métodos de leitura e configuração;
- eventos de mudança de estado;
- distinção entre estado válido, indisponível e falho;
- documentação Lua e testes automatizados do contrato público.

Exemplos de escopo por funcionalidade:

- a mesa modular expõe slots, módulos e o item de teste;
- a rede de cabos expõe conectividade, componentes descobertos e falhas;
- cada sensor expõe sua leitura, validade e estado de conexão;
- o GPWS expõe modos, alertas, severidades, thresholds e eventos;
- os módulos finais do painel expõem seu canal, threshold e estado de atuação.

## 6. Requisitos não funcionais

### Performance

- scans executados a cada 5 ticks por padrão;
- intervalo configurável;
- nenhum carregamento síncrono de chunks provocado por um scan;
- alcance máximo configurável;
- resolução do scanner avançado configurável;
- custo proporcional e previsível ao número de scanners ativos;
- resultados reutilizáveis entre o scanner, GPWS, painel e CC:Tweaked quando possível.

### Multiplayer e autoridade

- leituras relevantes para alertas devem ser calculadas de forma server-authoritative;
- estados de alerta devem ser sincronizados para clientes e painéis;
- componentes desconectados devem produzir estado explícito de falha;
- sensores dentro de sublevels devem usar posição e orientação corretas da contraption.

### Manutenibilidade

- separar aquisição de dados, rede de cabos, motor de regras e apresentação de alertas;
- manter a API CC:Tweaked estável e documentada;
- evitar acoplamento direto entre o GPWS e um único tipo de painel;
- preservar possibilidade de adicionar instrumentos futuros sem criar uma segunda rede de comunicação.

## 7. Baseline técnico

- Minecraft `1.21.1`;
- NeoForge `21.1.236`;
- Java 21;
- Sable `2.0.5`;
- Create Aeronautics `1.3+`;
- CC:Tweaked `1.120.0+`;
- Create: baseline de desenvolvimento atualmente configurada como `6.0.10+`, ainda sujeita à confirmação da versão mínima oficial.

Identidade atual do projeto:

- mod id: `aeronautics_fly_by_instruments`;
- pacote base: `com.radaeli.flightsafetyinstruments`;
- versão inicial do scaffold: `0.1.0`.

## 8. Requisito de testes automatizados

Testes automatizados são requisito obrigatório do desenvolvimento. Um build bem-sucedido sozinho não será considerado validação suficiente.

### Testes unitários

Devem cobrir, no mínimo:

- cálculo de clearance do Radar Altimeter;
- interpretação dos resultados do Terrain Scanner;
- seleção automática de fase de voo;
- thresholds de cada alerta;
- histerese e tempos de confirmação;
- prioridade e transição de estados de alerta;
- comportamento de `SENSOR FAIL`;
- perfis básico e avançado;
- serialização de configurações e thresholds.

Também devem cobrir a fundação da mesa modular:

- ocupação e liberação de slots;
- seleção e highlight do slot;
- colocação, remoção e serialização do item/módulo de teste;
- operação da mesa com canal simulado, sem GPWS real.

### Testes de rede de cabos

Devem cobrir:

- descoberta de sensores conectados;
- ramificações;
- desconexão e reconexão;
- múltiplos sensores do mesmo tipo;
- sensores ausentes ou indisponíveis;
- rede conectada a mais de um consumidor;
- persistência após salvar e recarregar o mundo.

### Testes de integração Minecraft/NeoForge

Devem validar:

- registro de blocos, itens, block entities e menus;
- carregamento do `neoforge.mods.toml`;
- montagem e leitura de contraptions/sublevels;
- transformação de posição e orientação dos sensores;
- exclusão da própria contraption dos scans;
- sincronização dos estados de alerta;
- compatibilidade com os JARs baseline.

### Testes de API CC:Tweaked

Devem validar automaticamente:

- presença dos tipos de periférico;
- nomes e assinaturas dos métodos;
- unidades e convenções dos valores retornados;
- leitura de sensores e alertas;
- configuração permitida por Lua;
- eventos de mudança de alerta;
- comportamento quando a rede está incompleta.

Esses testes devem acompanhar cada fase de funcionalidade. Não é aceitável
adiar a criação dos periféricos e eventos para uma fase de integração posterior.
O primeiro conjunto deve validar a mesa modular, seus slots e o item de teste.

### Testes de performance/regressão

Deve existir um cenário automatizado ou reproduzível que avalie:

- múltiplas contraptions com scanners ativos;
- frequência padrão de 5 ticks;
- scanner simples versus avançado;
- variação de alcance e resolução;
- ausência de carregamento inesperado de chunks;
- tempo de atualização do motor de alertas.

Os limites numéricos de performance serão definidos após existir um protótipo funcional e uma baseline medida.

Testes automatizados não substituem validação manual em mundo real. O comportamento de voo, a leitura visual do painel e a experiência de montagem ainda precisarão de testes in-game.

## 9. Roadmap macro

### Fase 0 — Fundação do projeto

Objetivo: deixar o projeto compilável e preparado para desenvolvimento.

Entregas:

- scaffold NeoForge funcional;
- metadados e dependências baseline;
- configuração de Java 21;
- estrutura de registries;
- harness inicial de testes;
- documentação de build e execução.

Saída da fase: projeto estruturalmente validado, com testes iniciais executando.

### Fase 1 — Annunciator Panel: fundação funcional (prioridade 1)

Objetivo: entregar primeiro uma mesa modular detalhada e funcional, mesmo sem
as funcionalidades finais de voo.

Primeira iteração, com escopo reduzido por decisão do jogador: apenas o bloco
`modular_table_base`, fino como um Copycat Panel (3/16 de bloco), com textura
placeholder de ferro, item no inventário criativo e colocação sobre superfícies.
Esta iteração permite testar a base no mundo.

Segunda iteração: grid virtual de 3×3 células por mesa, cada célula com
16/3 pixels de largura. A base pode ser montada em qualquer face sólida,
inclusive no teto. Os módulos de lâmpada 1×1 e 2×1 têm aparência placeholder e
não possuem comportamento de iluminação/alerta. O módulo 2×1 alterna entre
orientação horizontal e vertical pelo scroll enquanto o jogador mira na mesa.
O placement é escolhido pelo ponto de clique na face exposta, com highlight do
footprint e rejeição de sobreposição e de footprints fora dos limites. Uma
Wrench do Create retira o módulo. A ocupação persiste no mundo e é sincronizada
com os clientes. Cada mesa mantém seu próprio grid.

O peripheral `modular_table` acompanha essa entrega: consulta do grid, dos
módulos e da ocupação, preview de footprint, configuração de canal/threshold,
injeção de sinal simulado, consulta de saída e eventos de colocação/remoção,
configuração e mudança de sinal. Contratos e exemplos estão em
`docs/modular-table.md`.

Entregas:

- bases da mesa modular;
- slots predefinidos e regras de ocupação;
- colocação, remoção, seleção e highlight de slots;
- persistência da configuração;
- item/módulo de teste obtível pelo jogador;
- canal e saída simulados para validar a mesa isoladamente;
- peripheral, métodos e eventos CC:Tweaked da mesa e do item de teste;
- testes automatizados de slots, serialização, interação e API;
- consulta ao `reference/aeroworks-1.5.0.jar` durante o design da experiência
  modular.

Saída da fase: o jogador consegue criar uma mesa, inserir o item de teste,
editar slots e observar uma saída simulada, antes de existirem GPWS e sensores
finais.

### Fase 2 — Modelo comum e Avionics Cable Network

Objetivo: estabelecer os contratos de dados e a conexão física que sustentarão
os instrumentos, o GPWS e o painel.

Entregas:

- tipos de leituras de sensores;
- estados de conectividade e falha;
- modelo de alerta e severidade;
- modos do GPWS;
- serialização de configurações;
- Avionics Cable;
- conexão automática e ramificações;
- descoberta de sensores e consumidores;
- persistência e sincronização da rede;
- peripheral, métodos, eventos e testes CC:Tweaked da rede.

Saída da fase: uma rede física funcional sem depender de wireless, com contrato
de dados comum e API pública de conectividade.

### Fase 3 — Instrumentos de aquisição

Objetivo: produzir leituras confiáveis para o GPWS.

Entregas:

- Radar Altimeter;
- Terrain Scanner simples;
- Terrain Scanner avançado;
- integração com Velocity Sensor;
- integração com Gimbal Sensor;
- transformação correta de posição/orientação no sublevel;
- limites de alcance e resolução por configuração.
- peripherals, eventos e testes CC:Tweaked dos instrumentos e leituras.

Saída da fase: sensores testáveis individualmente por jogo e por CC:Tweaked.

### Fase 4 — Motor GPWS

Objetivo: transformar leituras em alertas.

Entregas:

- estados `STANDBY`, `ARMED` e `TEST`;
- seleção automática de fase de voo;
- alertas iniciais aprovados;
- thresholds padrão;
- histerese e tempos de confirmação;
- falhas e dados ausentes;
- perfil básico e perfil avançado.
- peripheral do GPWS;
- eventos de mudança de alerta e fase;
- métodos CC:Tweaked para leitura e configuração permitida;
- testes automatizados do contrato CC junto dos testes do motor.

Saída da fase: GPWS funcional sem depender ainda do painel final.

### Fase 5 — Annunciator Panel: integração com alertas reais

Objetivo: conectar a fundação da mesa aos alertas reais e completar os módulos
de apresentação.

Entregas:

- módulos de lâmpada;
- módulos de buzzer;
- ligação de módulos a canais;
- thresholds por módulo;
- estados de teste, acknowledge/mute se aprovados;
- testes de painel e estados de falha;
- eventos, métodos e testes CC:Tweaked dos módulos e canais reais.

Saída da fase: cabine funcional para jogadores básicos e avançados.

### Fase 6 — Estabilização e release inicial

Objetivo: preparar uma versão jogável e sustentável.

Entregas:

- testes automatizados completos;
- auditoria de cobertura dos contratos CC:Tweaked por funcionalidade;
- testes multiplayer;
- testes de performance;
- validação in-game de aeronaves pequenas e grandes;
- revisão de mensagens, Ponder e documentação;
- revisão de compatibilidade com as versões baseline;
- changelog e release notes.

Saída da fase: release inicial com escopo controlado e limitações conhecidas documentadas.

## 10. Critérios de aceite macro

O produto poderá ser considerado pronto para a primeira release quando:

1. o jogador conseguir conectar sensores por Avionics Cable sem wireless;
2. o GPWS detectar e publicar todos os alertas iniciais aprovados;
3. o modo básico funcionar sem configuração detalhada;
4. o modo avançado permitir thresholds e regras configuráveis;
5. o jogador conseguir criar a mesa modular, testar os slots e usar o item de
   teste antes da integração com o GPWS;
6. o Annunciator Panel exibir alertas através de módulos configuráveis;
7. cada funcionalidade for entregue com seus periféricos, eventos, APIs e
   testes CC:Tweaked, sem uma etapa posterior de integração pendente;
8. os testes automatizados cobrirem regras, rede, painel, integração e API;
9. scanners não provocarem carregamento inesperado de chunks;
10. o comportamento funcionar em contraptions dentro de sublevels;
11. a validação manual confirmar leitura compreensível e operação útil em voo.

## 11. Riscos e decisões abertas

- definir thresholds iniciais sem tornar os alertas irritantes;
- definir a detecção automática das fases de voo;
- estabelecer o significado exato de estruturas e outras contraptions como obstáculos;
- escolher alcance e resolução padrão do scanner avançado;
- tratar chunks não carregados sem gerar falsos negativos perigosos;
- definir os nomes, receitas e comportamento final dos blocos/itens de teste da
  mesa modular;
- definir os módulos finais do Annunciator Panel;
- confirmar a versão mínima exata de Create;
- validar os contratos atuais de Sable e Aeronautics no runtime;
- estabelecer metas mensuráveis de TPS após o primeiro protótipo.

## 12. Princípio de produto

O mod deve fazer uma aeronave parecer mais segura e mais instrumentada sem exigir que todos os jogadores construam um simulador de aviação completo. A profundidade deve existir como camada opcional; o caminho básico deve continuar sendo: posicionar, conectar, configurar minimamente e voar.
