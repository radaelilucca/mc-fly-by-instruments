# Flight Safety Instruments

## Status

Discovery e definição conceitual inicial. Este documento registra as decisões atuais para um futuro addon de segurança e instrumentação de voo para Create Aeronautics.

## Direção do projeto

O mod será inicialmente focado em **flight safety instruments**: instrumentos e sistemas que ajudam o piloto a perceber e evitar condições perigosas durante o voo.

O primeiro núcleo não terá como objetivo adicionar motores, autopilot completo ou um HUD genérico. A identidade inicial será segurança, consciência de terreno e integração física entre instrumentos.

Todos os componentes e módulos deverão nascer com uma API completa para **CC:Tweaked 1.120.0+**. A integração com computadores não será um recurso posterior ou opcional planejado depois do lançamento inicial; ela fará parte do contrato do mod desde o começo.

## Escopo inicial decidido

### GPWS

O **Ground Proximity Warning System** será o componente lógico responsável por interpretar dados dos sensores e gerar alertas de segurança.

Ele não será o sensor de terreno em si. O GPWS receberá dados da rede de avionics e decidirá quando uma condição deve produzir um alerta.

O GPWS inicial incluirá os modos de operação e contextos de voo definidos neste documento, mas sua configuração deverá ser simples por padrão. Jogadores que não quiserem simular todos os detalhes de um GPWS poderão instalar os sensores, conectar o cabo e usar thresholds automáticos.

Os alertas iniciais aprovados são:

- `TERRAIN`;
- `PULL UP`;
- `SINK RATE`;
- `DON'T SINK`;
- `TOO LOW`;
- `BANK ANGLE`;
- `SENSOR FAIL`.

### Interação simples e configuração avançada

O GPWS terá dois níveis de interação:

#### Modo básico

- modos de voo selecionados automaticamente;
- thresholds padrão definidos pelo mod;
- sensores descobertos automaticamente pela rede de cabos;
- canais de alerta disponíveis imediatamente para o Annunciator Panel;
- configuração reduzida a ativar/desativar o sistema e escolher um perfil global de sensibilidade.

Nesse modo, o jogador não precisa entender a lógica interna de cada modo de GPWS. A instalação deve ser próxima de: colocar o computador, posicionar os sensores, conectar o Avionics Cable e configurar o painel.

#### Modo avançado

- thresholds separados por modo e por alerta;
- tempos de confirmação e histerese;
- regras de arming/inhibit;
- seleção de sensores preferenciais ou redundantes;
- roteamento detalhado para os canais do Annunciator Panel;
- possibilidade de substituir a seleção automática da fase de voo.

O modo avançado deverá ampliar o sistema para jogadores interessados em simulação, sem tornar a configuração avançada obrigatória para o uso normal.

### Radar Altimeter

O **Radar Altimeter** fornecerá a distância entre a aeronave e a superfície diretamente abaixo dela, em altura relativa ao terreno (AGL).

Ele será diferente do `Altitude Sensor` original do Aeronautics, que fornece altitude global/pressão e velocidade vertical.

### Terrain Scanner

O **Terrain Scanner** será um bloco físico posicionado pelo jogador, preferencialmente no nariz da aeronave.

Em vez de um raycast único e reto, ele fará uma varredura em um **hemisfério frontal** a partir da posição e orientação do bloco.

O scanner deverá ser capaz de detectar terreno e possíveis obstáculos distribuídos à frente e lateralmente à trajetória. O alcance, resolução da varredura, tratamento de obstáculos e regras para terrenos não carregados ainda precisam ser definidos.

Haverá duas versões do scanner:

- **Scanner simples:** um raycast frontal único, de comportamento previsível e baixo custo.
- **Scanner avançado:** varredura em hemisfério frontal configurável, com abertura e resolução ajustáveis.

O alcance máximo dos scanners será limitado por configuração do mod.

O scanner deverá considerar como alvos relevantes o terreno, estruturas construídas e outras contraptions. A regra exata para classificar esses alvos ainda poderá ser refinada durante o design dos alertas.

### Sensores existentes do Aeronautics

`Velocity Sensor` e `Gimbal Sensor` serão integrados à rede de avionics como fontes de dados existentes.

O mod não deverá recriar esses sensores no escopo inicial. O GPWS poderá usar velocidade, velocidade vertical, pitch e roll junto com os dados do Radar Altimeter e do Terrain Scanner.

### Conexão física

A comunicação entre os componentes será feita por cabos físicos, sem wireless como mecanismo principal.

Haverá inicialmente um tipo de **Avionics Cable** que se conecta automaticamente a sensores compatíveis, ao GPWS e a futuros instrumentos/painéis.

Exemplo conceitual:

```text
[Terrain Scanner]──[Avionics Cable]──[GPWS]
                         └──[Radar Altimeter]
                         └──[Velocity Sensor]
                         └──[Gimbal Sensor]
```

Não haverá, no primeiro desenho, blocos separados de junction, data port ou frequency link. As ramificações dos próprios cabos formarão a rede.

O GPWS descobrirá automaticamente os sensores alcançáveis pela rede física. Um cabo rompido ou desconectado deverá resultar em sensor ausente ou falha de dados.

O Avionics Cable transportará dados. Alimentação de energia não fará parte do escopo inicial.

A rede deverá ser exposta de forma útil para CC:Tweaked, permitindo enumerar sensores, consultar leituras, observar falhas e acessar os canais de alerta do GPWS.

### Annunciator Panel

Haverá um **Annunciator Panel** para apresentar os alertas do GPWS e possivelmente de outros instrumentos.

Sua forma de funcionamento será modular, seguindo a ideia das mesas do Aeroworks:

- a mesa terá slots predefinidos;
- o jogador poderá escolher módulos, como lâmpadas e buzzers;
- o slot em edição será destacado visualmente;
- cada módulo poderá ser configurado para observar um canal do GPWS;
- cada módulo poderá ter thresholds de atuação configuráveis.

Ainda estão em aberto os módulos exatos, a forma de configuração dos canais, acknowledge/mute, teste de lâmpadas e integração com goggles ou displays.

## Arquitetura conceitual

```text
Sensores físicos
    ├─ Radar Altimeter
    ├─ Terrain Scanner
    ├─ Velocity Sensor
    └─ Gimbal Sensor
            │
            ▼
      Avionics Cable Network
            │
            ▼
        GPWS Computer
            │
            ▼
      Annunciator Panel
```

Os sensores devem ser posicionados dentro da contraption e acompanhar o sublevel. A posição e orientação do bloco terão significado físico: por exemplo, o Radar Altimeter deve ficar voltado para baixo e o Terrain Scanner deve ficar voltado para a frente.

O sistema deverá ignorar a própria contraption ao interpretar os scans, evitando que a fuselagem ou as asas sejam tratadas como obstáculos próprios.

## Decisões de design atuais

- O projeto será uma suite de segurança e instrumentação, não um pacote geral de aviação.
- GPWS é o interpretador de dados e gerador de alertas.
- O GPWS terá todos os modos de operação definidos, mas oferecerá uma experiência básica automatizada e uma configuração avançada opcional.
- Os alertas iniciais aprovados são `TERRAIN`, `PULL UP`, `SINK RATE`, `DON'T SINK`, `TOO LOW`, `BANK ANGLE` e `SENSOR FAIL`.
- Radar Altimeter mede clearance abaixo da aeronave.
- Terrain Scanner faz varredura em hemisfério frontal.
- O Terrain Scanner é um único bloco posicionável, sem exigir múltiplos sensores para cobrir o cone.
- A conexão principal será física, usando Avionics Cable.
- Cabos se conectam automaticamente a sensores compatíveis.
- `Velocity Sensor` e `Gimbal Sensor` existentes serão fontes de dados da rede.
- Annunciator Panel será modular, com slots predefinidos, módulos configuráveis e highlight do slot em edição; os módulos exatos e a interface final ainda estão em discovery.
- Wireless não será o mecanismo principal de conexão.
- Todos os módulos e sensores terão API completa para CC:Tweaked `1.120.0+` desde a primeira versão.

## Fora do escopo inicial

- motores e sistemas de propulsão;
- autopilot completo;
- substituição do `Altitude Sensor` original;
- radar de tráfego ou armamento;
- sistema logístico de rotas;
- HUD genérico sem relação direta com segurança de voo.

## Decisões registradas para a próxima etapa

### 1. Modos e alertas do GPWS

Esta pergunta separa duas coisas diferentes:

#### Modos de operação

São os estados ou contextos em que o GPWS trabalha. Por exemplo:

- `OFF` ou `STANDBY`;
- `ARMED`, monitorando o voo;
- `TEST`, acionando os canais para verificar o painel;
- fases de voo como decolagem, cruzeiro, descida e aproximação.

As fases de voo são importantes porque o mesmo comportamento pode ser normal em uma situação e perigoso em outra. Uma descida rápida durante a aproximação pode ser esperada; a mesma descida logo após a decolagem pode indicar perda de altitude perigosa.

#### Alertas

São os eventos individuais que o GPWS publica na rede e que o Annunciator Panel poderá apresentar. Exemplos possíveis:

- `TERRAIN`: terreno ou obstáculo relevante detectado à frente;
- `PULL UP`: risco iminente de colisão;
- `SINK RATE`: razão de descida excessiva;
- `DON'T SINK`: perda de altitude em uma fase em que a aeronave deveria estar subindo ou mantendo altitude;
- `TOO LOW`: clearance abaixo do limite configurado;
- `BANK ANGLE`: inclinação lateral excessiva;
- `SENSOR FAIL`: dados necessários ausentes ou inválidos.

Todos os modos de operação e contextos de voo listados acima farão parte do GPWS inicial. A seleção de modo será automática no modo básico e poderá ser ajustada manualmente no modo avançado.

Os alertas da lista acima também fazem parte do escopo inicial. Os thresholds exatos, tempos de confirmação e regras detalhadas de cada alerta ainda serão definidos.

### 2. Alvos do Terrain Scanner

O scanner detectará:

- terreno;
- estruturas construídas;
- outras contraptions.

Para o design atual, todos serão tratados como possíveis obstáculos físicos. A distinção entre terreno, estrutura e contraption poderá ser exposta como informação adicional ou filtro futuramente.

### 3. Resolução e alcance do Terrain Scanner

Haverá duas versões:

- scanner simples com raycast frontal único;
- scanner avançado com hemisfério frontal configurável.

O alcance máximo será controlado pelo arquivo de configuração do mod.

### 4. Dados transportados pelo Avionics Cable

O cabo transportará apenas dados. Energia não será trabalhada no escopo inicial.

### 5. Annunciator Panel

O painel será modular, inspirado nas mesas do Aeroworks. O jogador escolherá módulos, como lâmpadas e buzzers, e os posicionará em slots predefinidos. O slot em edição terá highlight visual.

Depois de instalado, cada módulo poderá ser configurado para observar canais do GPWS e atuar de acordo com thresholds definidos pelo jogador.

### 6. Frequência de atualização

Os scans serão executados a cada **5 ticks** por padrão. Esse intervalo será definido por configuração do mod.

### 7. Alvo técnico inicial

- Minecraft `1.21.1`;
- NeoForge;
- Sable `2.0.5`;
- Create Aeronautics `1.3+`.
- CC:Tweaked `1.120.0+`.

A versão exata de Create ainda não foi registrada.
