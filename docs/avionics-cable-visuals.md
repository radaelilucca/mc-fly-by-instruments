# Avionics Cable — encaixe visual nos sensores

## Escopo

O cabo tem modelo/texturas próprios e conector curto. Radar Altimeter e
Terrain Scanner Basic/Advanced fornecem pontos de encaixe adaptativo.
Mesa, Network Bridge e GPWS mantêm braços retos.
A conexão lógica continua automática em qualquer uma das seis faces; sockets
são detalhes visuais, não portas configuráveis nem uma restrição da rede.

## Pontos de apoio

Coordenadas locais em pixels (0–16), face vista a partir do sensor. O scanner é
modelado olhando NORTH; seus pontos acompanham a rotação horizontal do bloco.

| Face local | Radar Altimeter | Terrain Scanner Basic/Advanced |
| --- | --- | --- |
| NORTH | (5,5; 11; 0), socket de dados existente | (8; 8; 0,25), antena frontal |
| SOUTH | (8; 8; 15), carcaça | (8; 8; 16), socket traseiro existente |
| WEST | (1; 8; 8), carcaça | (1; 8; 8), carcaça |
| EAST | (15; 8; 8), carcaça | (15; 8; 8), carcaça |
| DOWN | (8; 0; 8), base da antena | (8; 2; 8), base entre os pés |
| UP | (8; 15,5; 3), tampa fora da placa | (8; 15; 4), tampa fora do emblema |

Nos sockets existentes a ponta encaixa no centro do socket, não no centro do
cubo Minecraft. Nas outras faces ela encosta na superfície da carcaça, sem
inventar sockets novos. Os conectores TX/RX decorativos do altímetro não são a
entrada de dados.

## Geometria e atualização

`CableSocketProvider` informa o ponto pela combinação de bloco, estado e face.
O modelo assado do cabo lê até seis estados vizinhos durante a reconstrução do
chunk. Mantém o hub, desvia a capa flexível até o ponto e conserva o terminal
perpendicular à face. O avanço axial é somente o recuo visual da superfície:
zero quando o socket já está na borda. Não há penetração, cap coplanar na ponta
nem renderizador de block entity por frame. Não há ticker ou pacotes novos.

O cliente usa a vizinhança disponível do chunk; a forma de seleção/colisão usa
os mesmos pontos e caixas curtas conservadoras. Não se solicita carga de chunks
para construir o visual. Alterar blocos ou sua orientação exige reconstruir a
geometria local, não recalcular continuamente por tick.

Fonte: [Blockbench](../blockbench/avionics-cable/README.md). O som deixa de ser o
de corrente e usa `SoundType.NETHERITE_BLOCK`, metálico e mais abafado. Não há
cópia de código ou assets do CC:Tweaked/Create.

## Verificação

Testes automatizados verificam todos os pontos/rotações, ponta exatamente na
superfície, ausência de avanço além dela, continuidade da capa/terminal, formas
que incluem a geometria, escopo dos blocos, texturas/UVs e som. Isso não substitui
a inspeção visual in-game.

Checklist manual após reiniciar o cliente:

- Cabo no socket deslocado NORTH do altímetro; conferir de frente e de lado.
- Cabo no socket traseiro do Basic e Advanced nas quatro orientações; conferir alinhamento.
- Contatos nas outras faces, especialmente tampa/base e antena rebaixada.
- Colocar/remover/girar sensor com o cabo já presente e recarregar o mundo.
- Ramificações de cabo/cabo e contato com bridge/mesa continuam retos.
- Repetir na fronteira de chunks, com Sodium e, separadamente, Iris/shader.
- Confirmar que a topologia CC:Tweaked permanece igual e avaliar o novo som.
