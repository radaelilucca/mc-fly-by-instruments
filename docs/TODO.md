# TODOs de longo prazo

Backlog central de melhorias e integrações ainda pendentes, consolidado em
2026-10-03. Os itens abaixo registram trabalho futuro; os componentes podem
continuar funcionando no estado atual enquanto não forem tratados.

## Renderização e conexão física

- [ ] **CABO-01 — Investigar os glitches angulares na textura do Avionics Cable.**
  O artefato parece z-fighting e muda conforme o ângulo de visão. Reproduzir e
  localizar a origem antes de escolher a correção; validar a aparência com e sem
  shaders e em diferentes ângulos.
- [ ] **CABO-02 — Levar o socket do Radar Altimeter para o centro.**
  Reposicionar o socket/ponto físico de conexão na face e manter o modelo, o
  encaixe visual do cabo e sua forma de seleção coerentes com essa posição.
- [ ] **CABO-03 — Retirar o desvio flexível do Avionics Cable.**
  Remover o caminho curvo e adotar conexão reta. O cabo deve terminar alinhado ao
  socket centralizado do altímetro. O encaminhamento flexível poderá ser
  reconsiderado depois, quando houver um desenho e uma implementação que se
  encaixem melhor.
- [ ] **CABO-04 — Revalidar visualmente o cabo nos sensores e orientações.**
  Conferir encaixe, junções e aparência após as mudanças do socket e do cabo,
  inclusive nas faces laterais, na traseira do Terrain Scanner, em sublevels e
  perto de fronteiras de chunks.
- [ ] **LÂMPADA-01 — Investigar os glitches das lâmpadas com shaders.**
  Há cintilação visual aleatória relatada com Complementary Reimagined Ultra;
  mudar para o perfil Basic não resolveu. Identificar a causa e conferir que
  módulos ligados e desligados continuam distinguíveis individualmente.

## Funcionalidades e integração

- [ ] **BUZZER-01 — Implementar o comportamento do módulo de buzzer.**
  Definir e adicionar som, associação com os canais de alerta e opções de
  configuração. Hoje o módulo pode ser instalado na mesa, mas não emite som nem
  responde a sinais.
- [ ] **PAINEL-01 — Conectar os módulos da mesa aos alertas reais do GPWS.**
  Permitir escolher o canal e configurar o threshold dos módulos compatíveis;
  incluir uma configuração rápida com defaults úteis para quem prefere montar e
  voar sem ajustar cada slot. Preservar a configuração manual para entusiastas.
- [ ] **GPWS-01 — Integrar as leituras do Terrain Scanner aos alertas do GPWS.**
  O scanner publica contatos e cobertura, mas ainda não participa da lógica de
  alertas. Definir como cobertura parcial e dados indisponíveis afetam os canais
  dependentes do terreno.
- [ ] **LÂMPADA-02 — Avaliar variantes de cor das lâmpadas.**
  Usar as mesmas proporções e materiais da variante vermelha; validar cada
  combinação acesa/apagada no jogo antes de fixar uma paleta adicional.

## Ideias futuras — FMS e navegação

Registro inicial para evoluir depois; ainda sem desenho técnico ou compromisso
com uma fase do roadmap.

- [ ] **FMS-01 — Adicionar um módulo de FMS como memória de waypoints.**
  Prover uma interface para o jogador criar e gerenciar waypoints, incluindo
  listagem e seleção. Persistir os waypoints para que sejam preservados após
  salvar e recarregar o mundo.
- [ ] **FMS-02 — Adicionar o item FMS Navigator / FMS Compass (nome provisório).**
  O item poderá ser colocado em uma Navigation Table. Se ela estiver em um
  sublevel que tenha o módulo de FMS, permitir que o jogador selecione no FMS
  um waypoint como destino ativo.
- [ ] **FMS-03 — Usar o waypoint ativo como target do compass da Navigation Table.**
  Enquanto o FMS estiver ativo e houver um waypoint selecionado, a Navigation
  Table com o item deverá orientar seu compass para esse destino. Detalhar
  futuramente a associação entre mesa e FMS, o comportamento sem destino ou
  com FMS desativado e as regras para múltiplos módulos no mesmo sublevel.

## Validação antes da release

- [ ] **QA-01 — Registrar validação visual in-game dos modelos e conexões.**
  Conferir colocação, inventário, orientação, sockets, shaders e leitura visual
  em mundo estático e em sublevels. Builds e testes automatizados, sozinhos, não
  substituem essa verificação.
- [ ] **QA-02 — Medir desempenho de scans e rede em condições representativas.**
  Avaliar scanners repetidos, Advanced no limite de raios, redes extensas,
  carregamento de chunks e contraptions ativas; manter scans sem forçar a carga
  de chunks.
- [ ] **QA-03 — Fazer testes de voo e multiplayer de ponta a ponta.**
  Exercitar aeronaves pequenas e grandes, alertas básicos e avançados, cobertura
  parcial, montagem do painel, eventos e peripherals antes da release inicial.

## Referências

- [Identidade visual e validação de shaders](visual-design-guide.md)
- [Encaixe visual do Avionics Cable](avionics-cable-visuals.md)
- [Mesa modular e módulos](modular-table.md)
- [Terrain Scanner e seus estados](terrain-scanner.md)
- [Critérios macro e roadmap](../PRD.md)
