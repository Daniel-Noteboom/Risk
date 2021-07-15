import java.util.*;
public class Game {

   //How many cards of "All" are there in the game
    public static final int CARD_TYPE_ALL_COUNT = 2;
    public static final int MAX_CARDS = 5;
    public static final int CARD_TURN_IN_SIZE = 3;
    public static final int BONUS_TROOPS_COUNTRY = 2;
    private boolean needTurnInCards = false;
    private boolean turnInCardsTest = false;
    //Keeps track of the different cards.
    private Queue<Card> deck = new LinkedList<>();

    private List<Card> discardPile = new ArrayList<>();

   //Variables dealing with phase of game
    public enum Phase {
        DRAFT,
        ATTACK,
        FORTIFY,
        PREGAME,
        ENDGAME
    }
    //The game starts in the pregame phase. This is the phase when the countries are populated. Once the game
    //Exits this phase, it can never reenter it unless there is a new game.
   private Phase currentPhase = Phase.PREGAME;

   private final Board board;

   //Variables dealing with Players
   private Player[] players;
   private static final int MINIMUM_PLAYERS = 3;
   private int startingPlayerIndex;

   //Keeps track of which players turn it is
   private int currentPlayerIndex;

   //Keeps track of how many troops are left in the draft phase
   private int currentReinforceTroopsNumber;

   //Need to keep track of whether it is the first turn (first turn means first turn for all players)
   private boolean firstTurn = true;

   //Keeps track of post-attack situation
   private int minimumTroopsDefeatedCountry = 0;
   private String currentDefeatedCountry;
   private String currentVictorCountry;

   //Keep track of current attack situation
   private String currentAttackCountry;
   private String currentDefendCountry;

   //Variables dealing with adding troops on first turn
   private static final int[][] FIRST_TURN_TROOPS = {{0,0,1}, {0,0,0,1}, {0,0,0,1,2}, {0,0,0,1,2,3}};
   private static final int MINIMUM_TROOPS_ADDED = 3;
   private static final int COUNTRY_TROOP_DIVISOR = 3;
   private static final int[] STARTING_TROOPS = {35, 30, 25, 20};

   //Variables dealing with dice number
   private static final int MAXIMUM_ATTACK = 3;
   private static final int MAXIMUM_DEFENSE = 2;
   public static final int DICE_SIDES = 6;

   private RiskController rC;
   private boolean usingRC;


    public Game(Board board, Player[] players, int startingPlayer) {
      this(board, players, startingPlayer, false, null);
    }

    //Creates the game, given the board, players, and a starting player
    //Set up manually refers to whether the player already has the board preloaded with troops
    public Game(Board board, Player[] players, int startingPlayer, boolean setUpManually, RiskController rC) {
        this.board = board;
        this.players = players;
        startingPlayerIndex = startingPlayer;
        currentPlayerIndex = startingPlayerIndex;
        this.rC = rC;
        usingRC = rC == null ? false : true;
        if(setUpManually) {
            changePhase();
        }
        createDeck();
    }

    public void resetGame(Player[] players, int startingPlayer, boolean setUpManually) {
        this.players = players;
        this.startingPlayerIndex = startingPlayer;
        this.currentPlayerIndex = startingPlayerIndex;
        if(setUpManually) {
            currentPhase = Phase.DRAFT;
        } else {
            currentPhase = Phase.PREGAME;
        }
    }
    /**
     * For testing purposes only, manually change what you want the deck to be. Should only be used at the beginning of the game,
     * otherwise it may create strange behavior
     */
    public void manuallyChangeDeck(List<Card> cards) {
        deck.clear();
        deck.addAll(cards);
    }

    /**
     * Helper method that creates the deck for the game. Creates deck with the number of countries and some additional
     * cards that are of all type category (number is et by the CARD_TYPE_ALL_COUNT field). Each country that is not ALL
     * is represented as a part of the card and that country may get additional troops if the current player owns that country
     * (only one country allowed per turn in of cards)
     */
    private void createDeck() {
        Set<String> setCountryNames = board.countryNames();

        List<String> countries = new ArrayList<>(setCountryNames);

        List<Card> unshuffledCards = new ArrayList<Card>();
        int countriesSize = countries.size();
        for(int i = 0; i < countriesSize; i++) {
            unshuffledCards.add(new Card(Card.cardTypes()[i % Card.cardTypes().length],
                                        countries.remove((int) (Math.random() * countries.size()))));
        }
        for (int i = 0; i < CARD_TYPE_ALL_COUNT; i++) {
            unshuffledCards.add(new Card(Card.Type.ALL, null));
        }
        for(int i = 0; i < setCountryNames.size() + CARD_TYPE_ALL_COUNT; i++) {
            deck.add(unshuffledCards.remove((int) (Math.random() * unshuffledCards.size())));
        }
        int i = 0;

    }

    /**
     * Determines if there is a country that will get additional troops if these cards were turned in on the current players .
     * Assumes that the indexes are correct (the actual turn in cards method checks for errors)
     * @param cardIndexes The desired indices of the cards of the current player to be turned in.
     * @return The country that will get additional troops if there is one, otherwise return an empty string. (If the indices are not possible
     * an empty string is returned)
     */
    public String extraCountryCardTurnIn(List<Integer> cardIndexes) {
        if(turnInCardsErrorHandling(cardIndexes)) {
            for(int i = 0; i < cardIndexes.size(); i++) {
                Card currentCard = players[currentPlayerIndex].getCards().get(cardIndexes.get(i));
                if (board.getOccupant(currentCard.getCountry()).equals(players[currentPlayerIndex])) {
                    return currentCard.getCountry();
                }
            }
        }
        return "";
    }

    /**
     * Determines if there are cards that can be turned in, and if there is, returns the indexes of these cards. Assumes that
     * you are looking for cards for the current given player, and does not check to make sure that you are in the right phase to
     * turn in cards, but simply looks at the available cards in the deck
     * @return Sample cards that can be turned in, or an empty list if it is not possible to turn in cards.
     */
    public List<Integer> turnInCards() {
        Map<Card.Type, List<Integer>> cardTypeCount = new HashMap<>();
        List<Card> cards = players[currentPlayerIndex].getCards();
        for(int i = 0; i < cards.size(); i++) {
            if(!cardTypeCount.containsKey(cards.get(i).getType())) {
                List<Integer> indexList = new ArrayList<>();
                indexList.add(i);
                cardTypeCount.put(cards.get(i).getType(), indexList);
            } else {
                cardTypeCount.get(cards.get(i).getType()).add(i);
            }
        }
        int allTypeCount = 0;

        if(cardTypeCount.containsKey(Card.Type.ALL)) {
            allTypeCount = cardTypeCount.get(Card.Type.ALL).size();
        }

        List<Integer> returnIndexList = new ArrayList<>();

        //You have at least one set of cards that are all different, so grab one of each (using as little ALL cards as possible)
        if(cardTypeCount.keySet().size() >= CARD_TURN_IN_SIZE || (cardTypeCount.keySet().size() + (allTypeCount - 1)) >= CARD_TURN_IN_SIZE) {
            for(Card.Type type: cardTypeCount.keySet()) {
                if (type != Card.Type.ALL) {
                    returnIndexList.add(cardTypeCount.get(type).get(0));
                }
            }
            int allIndex = 0;
            while(returnIndexList.size() != CARD_TURN_IN_SIZE) {
                returnIndexList.add(cardTypeCount.get(Card.Type.ALL).get(allIndex));
                allIndex++;
            }
            return returnIndexList;
        }
        //Get three of a kind, not using all card if possible)
        for(Card.Type type: cardTypeCount.keySet()) {
            if(cardTypeCount.get(type).size() + allTypeCount >= CARD_TURN_IN_SIZE) {
                returnIndexList = cardTypeCount.get(type).subList(0, Math.min(CARD_TURN_IN_SIZE, cardTypeCount.get(type).size()));
            }
            if(returnIndexList.size() != CARD_TURN_IN_SIZE) {
                int allIndex = 0;
                while (returnIndexList.size() != CARD_TURN_IN_SIZE && allTypeCount > allIndex) {
                    returnIndexList.add(cardTypeCount.get(Card.Type.ALL).get(allIndex));
                    allIndex++;
                }
            }
            if(returnIndexList.size() == CARD_TURN_IN_SIZE) {
                return returnIndexList;
            }
        }
        return new ArrayList<>();
    }

    public int remainingDeck() {
        return deck.size();
    }
    private Card getNextCard() {
        if(deck.size() == 0) {
            shuffleDeck();
        }
        return deck.remove();
    }

    /**
     * Shuffles deck is the current deck is empty. Cards in the discard pile are randomly placed back into the deck.
     * @return True if the shuffle was successful (there aren't any cards left in deck), false otherwise
     */
    private boolean shuffleDeck() {
        if(deck.size() != 0) {
            System.err.println("You should not shuffle for a new deck unless deck is empty");
            return false;
        }
        int discardPileSize = discardPile.size();
        for(int i = 0; i < discardPileSize; i++) {
            deck.add(discardPile.remove((int) (Math.random() * discardPile.size())));
        }
        return true;
    }

    public List<RiskView> getRiskViews() {
        return rC.getRiskViews();
    }

    //Returns list of player's names in order of when they start
    public List<String> getPlayerOrder() {
        List<String> playerNamesInOrder = new ArrayList<>();
        for(int i = startingPlayerIndex; i < startingPlayerIndex + players.length; i++) {
            playerNamesInOrder.add(players[i % players.length].getName());
        }
        return playerNamesInOrder;
    }
    //Get Troop Count  for the country
    public int getTroopCount(String country) {
      return board.getTroopCount(country);
    }

    //Get occupant of the current country (currently only used for tests)
    public String getOccupantName(String country) {
        return board.getOccupant(country).getName();
    }

    //Returns the name of the current player
    public String getCurrentPlayerName() {
        return players[currentPlayerIndex].getName();
    }

    //Returns the current phase
    public Phase getCurrentPhase() {
        return currentPhase;
    }

    //Gets occupant of given country
    private Player getOccupant(String country) {
        return board.getOccupant(country);
    }

    //Returns the current number of reinforcement troops that remain (will return 0 if it is not DRAFT phase)
    public int getCurrentReinforcementTroopsNumber() {
        return currentReinforceTroopsNumber;
    }

    public boolean isAttackOver() {
        return minimumTroopsDefeatedCountry != 0 || getTroopCount(currentAttackCountry) == 1;
    }
    public int getAttackDiceLimit(String country) {
        return Math.min(3, board.getTroopCount(country) - 1);
    }

    public int getNumberDefendDice(String country) {
        return Math.min(2, board.getTroopCount(country));
    }
    //Returns bordering countries with different occupant of concerned country
    public Set<String> getOpposingCountries(String country) {
        return board.opposingCountries(country);
    }

    public int getMinimumTroopsDefeatedCountry() {
        return minimumTroopsDefeatedCountry;
    }

    public String getCurrentVictorCountry() {
        return currentVictorCountry;
    }

    public String getCurrentDefeatedCountry() {
        return currentDefeatedCountry;
    }
    /** Returns and sets the number of troops that the current player can reinforce with
     * If the game phase is not correct 0 is returned **/
    private int setInitialReinforceTroopNumber() {
        if(!(currentPhase == Phase.DRAFT)) {
            return 0;
        }
        int extraTroops = 0;

        if(firstTurn) {
            extraTroops = FIRST_TURN_TROOPS[players.length - MINIMUM_PLAYERS][currentPlayerIndex];
        }
        currentReinforceTroopsNumber = Math.max(board.countriesOccupied(players[currentPlayerIndex]).size() / COUNTRY_TROOP_DIVISOR,
                                                MINIMUM_TROOPS_ADDED) + extraTroops + board.troopsFromContinents(players[currentPlayerIndex]);
        return currentReinforceTroopsNumber;
    }

    //Helper method that handles errors for reinforceTroops (refer to that method for what errors it flags)
    private boolean reinforceTroopsErrorHandling(int troops, String country) {
        if(!(currentPhase == Phase.DRAFT || (currentPhase == Phase.ATTACK && currentReinforceTroopsNumber > 0))) {
            System.err.println("ERROR: Current phase: " + currentPhase);
            System.err.println("Expected phase: " + Phase.DRAFT);
            return false;
        } else if(troops > currentReinforceTroopsNumber) {
            System.err.println("ERROR: Too many troops requested for draft");
            System.err.println("Requested troops: " + troops);
            System.err.println("Actual troops: " + currentReinforceTroopsNumber);
            return false;
        } else if(board.getOccupant(country) != players[currentPlayerIndex]) {
            System.err.println("ERROR: Current player doesn't have control over " + country);
            System.err.println("Current player: " + players[currentPlayerIndex]);
            System.err.println("Actual occupant: " + board.getOccupant(country));
            return false;
        } else if(needTurnInCards) {
            System.err.println("ERROR: You must turn in cards before reinforcing");
            return false;
        }
        return true;
    }

    /**
     * Checks for errors when player wants to turn in certain cards. Checks for errors that
     * can be checked for before looking at each card, but not errors that can only be determined
     * after looking at each card
     * @param turnInCards Indices representing the cards that player wants to turn in
     * @return False if preconditions not met (specified in regular method)
     * Returns true if all of the conditions are met.
     *
     */
    private boolean turnInCardsErrorHandling(List<Integer> turnInCards) {
        List<Card> playerCards = new ArrayList<>(players[currentPlayerIndex].getCards());
        if (playerCards.size() < CARD_TURN_IN_SIZE) {
            System.err.println("ERROR: You must have at least " + CARD_TURN_IN_SIZE + " cards to turn in");
            System.err.println("Current number of cards " + players[currentPlayerIndex].getCards().size());
            return false;
        } else if(turnInCards.size() != CARD_TURN_IN_SIZE) {
            System.err.println("ERROR: You must turn in the correct amount of cards");
            System.err.println("Card number to turn in: " + CARD_TURN_IN_SIZE);
            System.err.println("Cards attempted to turn in " + turnInCards.size());
            return false;
        } else if(currentPhase != Phase.ATTACK && currentPhase != Phase.DRAFT) {
            System.err.println("ERROR: You can only turn in cards in draft or attack phase");
            System.err.println("Current phase: " + currentPhase);
            return false;
        } else if(currentPhase == Phase.ATTACK && playerCards.size() < MAX_CARDS) {
            System.err.println("ERROR: You can only turn in cards on attack phase if you have at least " + MAX_CARDS);
            System.err.println("Current number cards: " + playerCards.size());
            return false;
        } else if(new HashSet<>(turnInCards).size() != CARD_TURN_IN_SIZE) {
            System.err.println("ERROR: You can't turn in the same card twice");
            return false;
        }
        return true;
    }

    private void setNeedTurnInCards() {
        if(players[currentPlayerIndex].getCards().size() >= MAX_CARDS) {
            needTurnInCards = true;
        }
    }

    /**
     * Returns the cards that a player has
     * in which case only the Type is returned
     * @param player The given player to get the cards for
     * @return A string representation of player's cards in the form of TYPE (COUNTRY) unless it is the ALL type
     */
    public List<String> getCards(String player) {
        List<String> cardContents = new ArrayList<>();
        List<Card> cards = getPlayer(player).getCards();
        for(int i = 0; i < cards.size(); i++) {
            cardContents.add(cards.get(i).toString());
        }
        return cardContents;
    }

    public Map<String, Integer> getCardsToIndexes(String player) {
        Map<String, Integer> cardsToIndexes = new HashMap<>();
        List<Card> cards = getPlayer(player).getCards();
        for(int i = 0; i < cards.size(); i++) {
            cardsToIndexes.put(cards.get(i).toString(), i);
        }
        return cardsToIndexes;
    }

    /**
     * Turns in cards for the given player. Updates reinforcements and troops for corresponding country if successful
     * as well as removing cards from players hand and placing in discard pile. Also updates whether the player continues
     * to need to turn in cards.
     * @param turnInCardsIndexes The suggested indicies of cards to be turned in for (assume current player is turning in cards)
     * @return true if turning in cards is successful
     * False if it is not by below conditions
     * 1. Player doesn't have enough cards
     * 2. Not right amount of cards that player is wanting to turn in for the given phase
     * 3. Wrong phase for turning in cards
     * 4. Indices are repeated for the desired turninCards
     * 5. Cards can't be turned in (not all same or all different)
     *
     */
    public boolean turnInCards(List<Integer> turnInCardsIndexes) {
        if(turnInCardsTest) {
            System.out.println("Testing cards started");
        }
        boolean noErrorsTurnInCards = turnInCardsErrorHandling(turnInCardsIndexes);
        if(!noErrorsTurnInCards) {
            if(turnInCardsTest) {
                System.out.println("Testing cards finished");
            }
            return false;
        }
        Set<Card.Type> typeSeen = new HashSet<>();
        boolean allDifferent = true;
        boolean allSame = false;
        //Create a copy of the players cards
        List<Card> playerCards = new ArrayList<>(players[currentPlayerIndex].getCards());
        String firstCountryOccupied = null;
        List<Card> turnInCards = new ArrayList<>();
        int allTypeCardsSeen = 0;
        for(int i: turnInCardsIndexes) {
            if(i >= playerCards.size()) {
                System.err.println("ERROR: Player does not have that many cards");
                System.err.println("Desired card number: " + (i + 1));
                System.err.println("Actual number cards: " + playerCards.size());
                if(turnInCardsTest) {
                    System.out.println("Testing cards finished");
                }
                return false;
            }
            Card currentCard = playerCards.get(i);
            turnInCards.add(currentCard);
            if(firstCountryOccupied == null && board.getOccupant(currentCard.getCountry()).equals(players[currentPlayerIndex])) {
                firstCountryOccupied = currentCard.getCountry();
            }
            if(currentCard.getType() != Card.Type.ALL && typeSeen.contains(currentCard.getType())) {
                allDifferent = false;
            }
            if(currentCard.getType() == Card.Type.ALL) {
                allTypeCardsSeen++;
            }
            typeSeen.add(currentCard.getType());
        }
        if((typeSeen.size() == 1 && !typeSeen.contains(Card.Type.ALL)) ||
                (typeSeen.size() == 2 && typeSeen.contains(Card.Type.ALL) && !allDifferent)) {
            allSame = true;
        }
        if(!(allDifferent || allSame)) {
            System.err.println("ERROR: Cards must either all be the same or all be different");
            System.err.println("Types seen: " + typeSeen);
            if(turnInCardsTest) {
                System.out.println("Testing cards finished");
            }
            return false;
        }
        if(turnInCardsTest) {
            System.out.println("Testing cards finished");
            return true;
        }
        if(allDifferent) {
            currentReinforceTroopsNumber += Card.getValue(Card.Type.ALL);
        } else {
            for(Card.Type type: typeSeen) {
                if(type != Card.Type.ALL) {
                    currentReinforceTroopsNumber += Card.getValue(type);
                    break;
                }
            }
        }
        if(firstCountryOccupied != null) {
            board.increaseTroops(firstCountryOccupied, BONUS_TROOPS_COUNTRY);
        }
        for(Card c: turnInCards) {
            discardPile.add(c);
            players[currentPlayerIndex].getCards().remove(c);
        }
        if(players[currentPlayerIndex].getCards().size() < MAX_CARDS && needTurnInCards) {
            needTurnInCards = false;
        }
        if(usingRC) {
            rC.update();
        }
        return true;
    }

    public boolean needTurnInCards() {
        return needTurnInCards;
    }

    /**
     * Determines whether you can turn in cards without actually turning them in
     * @param indexes The indexes of the cards in the current players hand to turn in
     * @return true if possible to turn in these cards, false otherwise
     */
    public boolean canTurnInCards(List<Integer> indexes) {
        turnInCardsTest = true;
        boolean canTurnInCards = turnInCards(indexes);
        turnInCardsTest = false;
        return canTurnInCards;
    }

    /**
     * Reinforce troops for the given country with the amount of troops specified. The country must be occupied by the current player,
     * The current phase must be draft, and the current player must have enough troops to reinforce that country with the specified amount of
     * troops.
     * @param troops Amount of troops to reinforce with
     * @param country Country specified
     * @return True if successful, false if one or more errors was present
     */
    public boolean reinforceTroops(int troops, String country) {
        boolean noErrorsReinforce = reinforceTroopsErrorHandling(troops, country);

        if(noErrorsReinforce) {
            board.increaseTroops(country, troops);
            currentReinforceTroopsNumber -= troops;
            if (currentReinforceTroopsNumber == 0 && currentPhase == Phase.DRAFT) {
                changePhase();
            }
            if(usingRC) {
                rC.update();
            }
        }
        return noErrorsReinforce;
    }

    /**
     * Returns the actual player given the name that was given.
     * @param player
     * @return Returns the player associated with the name. Otherwise returns a player with an empty name
     */
    private Player getPlayer(String player) {
       for(Player p: players) {
           if(p.getName().equals(player)) {
                return p;
           }
       }
       return new Player("");
    }

    /**
     * The only method where the game state can be changed manually by the user. This method should only be used
     * for testing purposes
     * @param player The desired player to switch to
     * @param phase The desire phase to switch to
     */
    public void changeGameState(String player, Phase phase) {
        for(int i = 0; i < players.length; i++) {
            if(players[i].getName().equals(player)) {
                currentPlayerIndex = i;
            }
        }
        currentPhase = phase;

        if(phase == Phase.DRAFT) {
            setInitialReinforceTroopNumber();
            setNeedTurnInCards();
        } else {
            currentReinforceTroopsNumber = 0;
        }
    }

    /**
     * Performs on attack by attackCountry on defendCountry using the dice, attackDice and defendDice respectively. This method
     * will update the current troops in each country if there are no errors in the attack. The attacker takes over the defend
     * country if there are no more troops left. If the attacker has control over every country, the game is over.
     * @param attackCountry The country that is attacking
     * @param attackDice The dice that the attacker has rolled
     * @param defendCountry The country that is defending
     * @param defendDice The dice that the defender has rolled
     * @return Whether the attack was successful or not (if there was errors with teh attack returns false). If one of these
     * conditions is not met, this method will return false
     *
     *     Conditions:
     *     1.  The length of attackDice must be a number from 1-3 and <= (attackCountry troops - 1) and attackCountry troops must be
     *     greater than one
     *     2.  attackCountry and defendCountry must be different players
     *     3.  attackCountry and defendCountry must be adjacent countries
     *     4. The board must contain these countries.
     *     5. The length of defend dice must be two if the defender has at least two troops, and one otherwise
     */
    public boolean attack(String attackCountry, List<Integer> attackDice, String defendCountry, List<Integer> defendDice) {
      if(!attackErrorHandling(attackCountry, attackDice, defendCountry, defendDice)) {
          return false;
      }
      currentAttackCountry = attackCountry;
      currentDefendCountry = defendCountry;
      List<Integer> sortedAttackDice = new ArrayList<Integer>(attackDice);
      List<Integer> sortedDefendDice = new ArrayList<Integer>(defendDice);
      Collections.sort(sortedAttackDice);
      Collections.sort(sortedDefendDice);

      //Whoever has the lowest highest die loses a troop
      if (sortedAttackDice.get(sortedAttackDice.size() - 1) > sortedDefendDice.get(sortedDefendDice.size() - 1)) {
         board.reduceTroops(defendCountry, 1);
      } else {
         board.reduceTroops(attackCountry, 1);
      }

      //If there are two dice in play by both attack/defender, whoever has the lowest second highest die loses a troop
      if (sortedAttackDice.size() > 1 && sortedDefendDice.size() > 1 && sortedAttackDice.get(sortedAttackDice.size() - 2) >
              sortedDefendDice.get(sortedDefendDice.size() - 2)) {
         board.reduceTroops(defendCountry, 1);
      } else if (sortedAttackDice.size() > 1 && sortedDefendDice.size() > 1) {
         board.reduceTroops(attackCountry, 1);
      }

      //Take over the country if there are no more troops
      if(board.getTroopCount(defendCountry) == 0) {
          players[currentPlayerIndex].setAttackThisTurn();
          Player occupantDefeated = getOccupant(defendCountry);
          board.changeOccupant(defendCountry, players[currentPlayerIndex], 0);
          if(board.playerFinished(occupantDefeated)) {
              occupantDefeated.setOut();
              for(Card card: occupantDefeated.getCards()) {
                  players[currentPlayerIndex].addCard(card);
              }
              if(players[currentPlayerIndex].getCards().size() >= MAX_CARDS) {
                  needTurnInCards = true;
              }
          }
          if(isGameOver()) {
              currentPhase = Phase.ENDGAME;
          } else {
              currentDefeatedCountry = defendCountry;
              currentVictorCountry = attackCountry;
              minimumTroopsDefeatedCountry = attackDice.size();
          }
      }
      if(usingRC) {
         rC.update();
      }
      return true;
    }

    //Determines whether the game is over (only one player remains)
    private boolean isGameOver() {
        int numberOut = 0;
        for(Player p: players) {
            if (p.isOut()) {
                numberOut++;
            }
        }
        return numberOut == players.length - 1;
    }
    /**
     * Moves the correct number of troops from attack country to defeated country. Does not wor kif the phase is not correct,
     * this attack did not just finish or if the number of troops is not at least as large as the amount of dice the attacker
     * used to attack the country and is less than the number of troops the attacker has in the attacking country
     * @param troops The number of troops to move.
     * @return Whether the attack was successful or not
     */
    public boolean setTroopsDefeatedCountry(int troops) {
        if(errorHandlingSetTroopsDefeatedCountry(troops)) {
            board.increaseTroops(currentDefeatedCountry, troops);
            board.reduceTroops(currentVictorCountry, troops);
            minimumTroopsDefeatedCountry = 0;
            currentVictorCountry = null;
            currentDefeatedCountry = null;
            if(usingRC) {
               rC.update();
            }
            return true;
        } else {
            return false;
        }
    }

    //Helper method that handles errors setTroopsDefeatedCountry. Refer to that method for details about the errors
    private boolean errorHandlingSetTroopsDefeatedCountry(int troops) {
        if(currentPhase != Phase.ATTACK) {
            System.err.println("You are not in attack phase so there is no troops to set for a defeated country");
            return false;
        } else if(minimumTroopsDefeatedCountry == 0) {
            System.err.println("There is no current defeated country");
            return false;
        } else if(troops < minimumTroopsDefeatedCountry || troops >= getTroopCount(currentVictorCountry)) {
            System.err.println("The number of troops requested is " + troops);
            System.err.println("The number of troops must be at least " + minimumTroopsDefeatedCountry);
            System.err.println("The number of troops must be less than " + getTroopCount(currentVictorCountry));
            return false;
        } else if(needTurnInCards) {
            System.err.println("You still need to turn in cards since you have at least " + MAX_CARDS);
            return false;
        } else if (currentReinforceTroopsNumber > 0) {
            System.err.println("You still need to reinforce since you have " + currentReinforceTroopsNumber +
                    " troops");
            return false;
        }
        return true;
    }



    //Handles errors for the attack method. For specifics on what is allowed/not allowed refer to the attack method
    private boolean attackErrorHandling(String attackCountry, List<Integer> attackDice, String defendCountry,
                                        List<Integer> defendDice) {
        //Refer to error messages for explanation of if statement
        if (currentPhase != Phase.ATTACK) {
            System.err.println("Phase is not correct. It should be attack phase but is actually " + currentPhase);
            return false;
        } else if (attackDice.size() < 1 || attackDice.size() > MAXIMUM_ATTACK || attackDice.size() >= getTroopCount(attackCountry)) {
            System.err.println("You can have 1-3 attack dice and the number of dice has to " +
                    "be less than the number of troops in the country");
            return false;
        } else if (getOccupant(attackCountry) != players[currentPlayerIndex]) {
            System.err.println("You can only attack if it's your turn");
            return false;
        } else if (defendDice.size() != Math.min(getTroopCount(defendCountry), MAXIMUM_DEFENSE)) {
            System.err.println("Defender must defend with either one or two dice (one dice if defender only has one troop)");
            return false;
        } else if (getOccupant(attackCountry) == getOccupant(defendCountry)) {
            System.err.println("You are not allowed to attack yourself");
            return false;
        } else if (!board.isBordering(attackCountry, defendCountry))  {
            System.err.println("You must attack an adjacent country");
            return false;
        } else if (!board.containsCountry(attackCountry) || !board.containsCountry(defendCountry)) {
            System.err.println("Board must contain the countries that are attacking");
            return false;
        } else if (needTurnInCards) {
            System.err.println("You need to turn in cards before attacking since you have more than " + MAX_CARDS + " cards");
        }
        return true;
    }

    /**
     * End the current attack phase. Only works if the game is in the right state
     * @return Whether the change of phase was successful
     */
    public boolean endAttackPhase() {
        if(errorHandlingEndAttackPhase()) {
            changePhase();
            return true;
        }
        return false;
    }

    //Helper method that tracks errors for endAttack (refer to that method for more details)
    private boolean errorHandlingEndAttackPhase() {
        if(currentPhase != Phase.ATTACK) {
            System.err.println("You must be in the attack phase to end the attack phase");
            return false;
        } else if(minimumTroopsDefeatedCountry != 0) {
            System.err.println("You still need to set troops for " + currentDefeatedCountry + " before you can end attack phase");
            return false;
        }
        return true;
    }

    /**
     * Calculates all the fortify possibilities for the current country
     *
     * @param country The name of the country to find the reinforcement possibilities for
     * @return Set<String> that contains the reinforcement possibilities as a Set of the country names
     */
    public Set<String> fortifyPossibilities(String country) {
        if(board.getOccupant(country) != players[currentPlayerIndex] || currentPhase != Phase.FORTIFY) {
          return new HashSet<String>();
       }
       return board.connectedCountries(country);

    }

    /**
     * Calculates and returns the names of the countries controlled by the given player
     * @param player The name of the player that we are finding the country names for
     * @return Set<String> that contains the country names controlled by the given player
     */
    public Set<String> getCountries(String player) {
        Set<Country> countries = board.countriesOccupied(getPlayer(player));
        Set<String> countryNames = new HashSet<>();
        for(Country c: countries) {
            countryNames.add(c.getName());
        }
        return countryNames;
    }

    //Handles errors for the fortify troops method
    private boolean fortifyTroopsErrorHandling(String countryFrom, String countryTo, int troops) {
        if(!(currentPhase == Phase.FORTIFY)) {
            System.err.println("ERROR: Current phase: " + currentPhase);
            System.err.println("Expected phase: " + Phase.FORTIFY);
            return false;
        } else if(troops >= board.getTroopCount(countryFrom)) {
            System.err.println("ERROR: Too many troops requested for reinforcements");
            System.err.println("Requested troops: " + troops);
            System.err.println("Actual troops: " + currentReinforceTroopsNumber);
            return false;
        } else if(board.getOccupant(countryFrom) != players[currentPlayerIndex]) {
            System.err.println("ERROR: Current player doesn't have control over " + countryFrom);
            System.err.println("Current player" + players[currentPlayerIndex]);
            System.err.println("Actual occupant + " + board.getOccupant(countryFrom));
            return false;
        } else if (board.getOccupant(countryTo) != players[currentPlayerIndex]) {
            System.err.println("ERROR: Current player doesn't have control over " + countryTo);
            System.err.println("Current player" + players[currentPlayerIndex]);
            System.err.println("Actual occupant + " + board.getOccupant(countryTo));
            return false;
        } else if(!board.connectedCountries(countryFrom).contains(countryTo)) {
            System.err.println("ERROR: Country " + countryTo + " Is not connected to " + countryFrom);
            return false;
        }
        return true;
    }

    private void addAttackCards() {
        if(players[currentPlayerIndex].attackThisTurn()) {
            players[currentPlayerIndex].addCard(getNextCard());
        }
    }

    /**
     * Fortify int troops from country "start" to country "end". Does not fortify if info provided is incorrect
     * @param countryFrom The country you're moving troops from
     * @param countryTo The country you're moving troops to
     * @param troops the amount of troops you're moving
     * @return Whether it was successful (there needs to be enough troops and the countries need to
     * match the right occupant
     */
    public boolean fortifyTroops(String countryFrom, String countryTo, int troops) {
        boolean noErrorsFortify = fortifyTroopsErrorHandling(countryFrom, countryTo, troops);
        if(noErrorsFortify) {
            board.reduceTroops(countryFrom, troops);
            board.increaseTroops(countryTo, troops);
            addAttackCards();
            changePhase();
            if(usingRC) {
                rC.update();
            }
        }
        return noErrorsFortify;
    }

    //Since a player can choose not to participate in the fortify phase, they may call this method to skip this phase.
    public void endFortifyPhase() {
        if(currentPhase == Phase.FORTIFY) {
            addAttackCards();
            players[currentPlayerIndex].unsetAttackThisTurn();
            changePhase();
        }
    }
    /**
     * Helper method that changes the phase of the board and any additional things that need to be done.
     */
    private void changePhase() {
        if(currentPhase == Phase.PREGAME) {
            currentPhase = Phase.DRAFT;
            setInitialReinforceTroopNumber();
            setNeedTurnInCards();
        } else if(currentPhase == Phase.DRAFT) {
            currentPhase = Phase.ATTACK;
        } else if(currentPhase == Phase.ATTACK) {
            currentPhase = Phase.FORTIFY;
        } else if(currentPhase == Phase.FORTIFY) {
            if(firstTurn && isFullTurnCycle()) {
                firstTurn = false;
            }
            currentPhase = Phase.DRAFT;
            nextPlayer();
            setInitialReinforceTroopNumber();
            setNeedTurnInCards();
        }
    }

    /**
    If it is outside those bounds then an empty List is returned
    Returns an List<Integer> with random numbers of 1-DICE_SIDES **/

    /**
     * Returns random dice rolls for "numberDice" times. The randomizer is set to return a random
     *      * Number from 1-DICE_SIDES
     * @param numberDice The number of dice to be rolled (must be a number from 0 to 1000)
     * @return The random dice result with a size of "numberDice". If numberDice is not correct an empty list is returned.
     */
    public static List<Integer> rollDice(int numberDice) {
      if (numberDice > 1000 || numberDice < 1) {
         return new ArrayList<Integer>();
      }
      List<Integer> diceRolls = new ArrayList<Integer>();
      for(int i = 0; i < numberDice; i++) {
         // Produces a random number from 1 to DICE_SIDES
         diceRolls.add((int) (Math.random() * DICE_SIDES + 1));
      }
      return diceRolls;
    }

    /**
     *     Creates a randomly populated board with the appropriate number of troops. The first player has priority.
     *     Each country is randomly populated according to the rules of Risk. No country can have more than one troop
     *     until all countries have been populated with at least one troop and troops are added one at at time beginning
     *     with the starting player. Clears the board if there is already troops there
     */
    public void randomlyPopulateBoard() {
        board.clearTroops();
        int numberTroopsAdd = STARTING_TROOPS[players.length - MINIMUM_PLAYERS];
        int countriesOccupied = 0;
        //Randomly populate the countries with one troop each, starting with the first player
        while (!(countriesOccupied == board.numberCountries())) {

            Country currentCountry = board.randomUnoccupiedCountry(countriesOccupied);

            currentCountry.changeOccupant(players[currentPlayerIndex], 1);

            countriesOccupied++;
            //Check to see if we have completed a full round of adding troops
            if(isFullTurnCycle()) {
                numberTroopsAdd--;
            }
            nextPlayer();


        }

        while (numberTroopsAdd != 0) {
            board.increaseTroops(board.randomCountry(players[currentPlayerIndex]), 1);
            if(isFullTurnCycle()) {
                numberTroopsAdd--;
            }
            nextPlayer();

        }

        //DELETE
        for(Player p: players) {
            if(players[startingPlayerIndex] != p) {
                while(board.countriesOccupied(p).size() != 1) {
                    board.changeOccupant(board.randomCountry(p), players[startingPlayerIndex], 10);
                }
            }
        }

        if(currentPhase != Phase.DRAFT) {
            changePhase();
        }
    }

    /** Helper method that returns whether all players have completed an entire cycle. This does not determine whether the final player
    in the cycle has completed their moves. The method should be called at the appropriate time (when the current player
    has finished their turn.**/
    private boolean isFullTurnCycle() {
        return currentPlayerIndex == (startingPlayerIndex == 0 ? players.length - 1 : startingPlayerIndex - 1);
    }

    //Helper method Changes board state so that is now the next player's turn.
    private void nextPlayer() {
        currentPlayerIndex = currentPlayerIndex == players.length - 1 ? 0 : currentPlayerIndex + 1;
        if(players[currentPlayerIndex].isOut()) {
            nextPlayer();
        }
    }
}