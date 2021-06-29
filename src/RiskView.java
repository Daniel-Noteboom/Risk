import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

public class RiskView extends JPanel {
    private Map<String, Point> countryCoordinates;
    private Image pic;
    private Game game;
    private Map<String, Color> playerColors;
    private static final int X_OFFSET_ONE_DIGIT = 9;
    private static final int X_OFFSET_TWO_DIGITS = 4;
    private static final int Y_OFFSET = 18;
    private static final int DIAMETER_CIRCLE = 25;
    private static final int OFFSET_LIST_PLAYERS_X = 30;
    private static final int OFFSET_LIST_CARDS_X = 10;
    private static final int OFFSET_LIST_CARDS_Y = 500;
    private static final int EXTRA_BOTTOM_SPACE = 100;
    private static final int OFFSET_INBETWEEN_TEXT = 14;
    private static final int OVAL_SPACING = 10;
    private static final int OFFSET_LIST_PLAYERS_Y = EXTRA_BOTTOM_SPACE;
    private static final int OFFSET_INFO = 200;
    private static final int INFO_OFFSET_FROM_MIDDLE = 100;
    private static final int EXTRA_CIRCLE_SIZE = 10;
    private static final int EXTRA_CIRCLE_THICKNESS = 3;
    private static final Color ATTACK_COLOR = Color.decode("#008000"); //Dark Green
    private static final Color DEFEND_COLOR = Color.decode("#800000"); //Dark Red
    private static final Color FORTIFY_COLOR = Color.BLACK;
    private static final Color FORTIFIED_COLOR = Color.YELLOW;
    private String currentAttackCountry;
    private String currentDefendCountry;
    private String currentFortifyCountry;
    private String currentFortifiedCountry;
    public RiskView(String fileName, Map<String, Point> countryCoordinates, Map<String, Color> playerColors,
                           Game game) {
        this.countryCoordinates = countryCoordinates;
        ImageIcon board = new ImageIcon(fileName);
        pic = board.getImage();
        this.game = game;
        this.playerColors = playerColors;
        this.setLayout(null);
    }

    public int getExtraBottomSpace() {
        return EXTRA_BOTTOM_SPACE;
    }

    public void notifyViewer() {
        repaint();
    }

    public int getHeight() {
        return pic.getHeight(this) + EXTRA_BOTTOM_SPACE;
    }

    public int getWidth() {
        return pic.getWidth(this);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(pic, 0, 0, null);
        drawCountryTroops(g);
        drawBottomBar(g);
        drawPhaseInformation(g);
    }

    public void mouseClicked(int x, int y) {
        //If you're not clicking on the component on draft or fortifyyou want to remove it
        if(getComponentCount() != 0 && game.getCurrentPhase() == Game.Phase.DRAFT) {
            clearAndReset(this);
        }
        boolean countryClicked = false;

        //We find whether we clicked on a country and if so handle things according to the phase that we are in
        for(String country: countryCoordinates.keySet()) {
            Point countryPoint = countryCoordinates.get(country);
            if(x >= countryPoint.getX() && x < countryPoint.getX() + DIAMETER_CIRCLE &&
                    y >= countryPoint.getY() && y < countryPoint.getY() + DIAMETER_CIRCLE) {
                if((game.getCurrentPhase() == Game.Phase.DRAFT ||
                   (game.getCurrentPhase() == Game.Phase.ATTACK && game.getCurrentReinforcementTroopsNumber() > 0)) &&
                        game.getCurrentPlayerName().equals(game.getOccupantName(country))) {
                    handleDraftMouseClick(country);
                } else if(game.getCurrentPhase() == Game.Phase.ATTACK) {
                    countryClicked = true;
                    handleAttackMouseClick(country);
                } else if(game.getCurrentPhase() == Game.Phase.FORTIFY &&
                        game.getCurrentPlayerName().equals(game.getOccupantName(country))) {
                    countryClicked = true;
                    handleFortifyMouseClick(country);
                }
            }
        }

        //Remove all JPanels if user didn't click on country and we're in attack phase, and there is not troops to move
        //All remove if fortify phase and there is not a current fortify country
        //There should only be max of one JPanel for the possible amount of troops to attack with
        if(((game.getCurrentPhase() == Game.Phase.ATTACK && game.getMinimumTroopsDefeatedCountry() == 0) ||
                game.getCurrentPhase() == Game.Phase.FORTIFY) && !countryClicked) {
            clearAndReset(this);
        }
    }

    //Helper method to make the box for the different methods. Since you can't pass
    //in a function in java there is an if statement for the time the box is used, but
    //all the other code is resusable
    private void optionBox(String country, JPanel panel, Integer[] options, String buttonText) {
        panel.setLocation(panelLocation(countryCoordinates.get(country)));
        panel.setLayout(new BorderLayout());
        JComboBox<Integer> comboBox = new JComboBox<>(options);
        panel.add(comboBox, BorderLayout.NORTH);
        JButton button = new JButton(buttonText);
        panel.add(button, BorderLayout.SOUTH);
        if(game.getCurrentPhase() == Game.Phase.ATTACK && game.getMinimumTroopsDefeatedCountry() == 0) {
            button.addMouseListener(new AttackClicked(comboBox, panel));
        } else {
            button.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int selectedNumber = (int) comboBox.getSelectedItem();
                    if (game.getCurrentPhase() == Game.Phase.DRAFT ||
                            (game.getCurrentPhase() == Game.Phase.ATTACK &&
                                    game.getCurrentReinforcementTroopsNumber() > 0)) {
                        game.reinforceTroops(selectedNumber, country);
                    } else if(game.getMinimumTroopsDefeatedCountry() != 0) {
                        game.setTroopsDefeatedCountry(selectedNumber);
                    } else if(game.getCurrentPhase() == Game.Phase.FORTIFY) {
                        game.fortifyTroops(currentFortifyCountry, currentFortifiedCountry, selectedNumber);
                    }
                    clearAndReset(panel.getParent());
                }
            });
        }
        panel.setSize(panel.getPreferredSize());
        if(panel.getParent() == null) {
            add(panel);
            revalidate();
        }
    }
    private void handleDraftMouseClick(String country) {
        Integer[] possibleTroops = new Integer[game.getCurrentReinforcementTroopsNumber()];
        for (int i = 0; i < possibleTroops.length; i++) {
            possibleTroops[i] = possibleTroops.length - i;
        }
        optionBox(country, new JPanel(), possibleTroops, "Confirm");
    }

    private void handleAttackMouseClick(String country) {

        //We need to check to make sure we're not moving troops, because if we are, we can't
        //take any further actions until the troops are moved.
        if((game.getMinimumTroopsDefeatedCountry() == 0)) {
            //If the country clicked belongs to the current player then we are resetting
            if(game.getOccupantName(country).equals(game.getCurrentPlayerName())) {
                clearAndReset(this);
                //We set currentAttackCountry to the country if we can actually attack (there is more
                //than one troop and there is bordering countries to attack)
                currentAttackCountry = game.getTroopCount(country) > 1 &&
                                        game.getOpposingCountries(country).size() != 0 ? country : null;
            //We've clicked on opposing country
            } else {
                //We actually have an attacking country, and the defending country is bordering
                //so we proceed with attack, given that we don't have another attack going on
                if(currentAttackCountry != null && currentDefendCountry == null &&
                        game.getOpposingCountries(currentAttackCountry).contains(country)) {
                    currentDefendCountry = country;
                    repaint();
                    setUpAttack();
                //We reset completely
                } else {
                    clearAndReset(this);
                }
            }
        }
    }

    //Helper method that sets up the attack given that attack & defend country are set
    private void setUpAttack() {
        int maxDice = game.getAttackDiceLimit(currentAttackCountry);
        Integer[] possibleDice = new Integer[maxDice];
        for(int i = 0; i < possibleDice.length; i++) {
            possibleDice[i] = possibleDice.length - i;
        }
        optionBox(currentAttackCountry, new JPanel(), possibleDice, "Attack");
    }

    //Class to handle the event where the users has confirmed an attack
    private class AttackClicked extends MouseAdapter {

        //Combo Box that has the number of attack dice the user has chosen
        JComboBox<Integer> attackDiceNumber;
        //The panel that contains the Combo box.
        JPanel attackPanel;

        public AttackClicked(JComboBox<Integer> attackDiceNumber, JPanel attackPanel) {
            this.attackDiceNumber = attackDiceNumber;
            this.attackPanel = attackPanel;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int numberAttackDice = (int) attackDiceNumber.getSelectedItem();
            java.util.List<Integer> attackDice = Game.rollDice(numberAttackDice);
            java.util.List<Integer> defendDice = Game.rollDice(game.getNumberDefendDice(currentDefendCountry));
            game.attack(currentAttackCountry, attackDice, currentDefendCountry, defendDice);
            if(game.isAttackOver()) {
                //The attack can be over for two reasons.
                //1. Attacker ran out of troops
                if(game.getMinimumTroopsDefeatedCountry() == 0) {
                    Container parent = attackPanel.getParent();
                    clearAndReset(parent);
                //2. Defender was defeated
                } else {
                    attackPanel.removeAll();
                    attackPanel.setName("Move troops");
                    int minTroops = game.getMinimumTroopsDefeatedCountry();
                    int maxTroops = game.getTroopCount(currentAttackCountry) - 1;
                    Integer[] possibleTroopsMove = new Integer[maxTroops - minTroops + 1];
                    for(int i = minTroops; i <= maxTroops; i++) {
                        possibleTroopsMove[i - minTroops] = maxTroops + minTroops - i;
                    }
                    optionBox(currentAttackCountry, attackPanel, possibleTroopsMove, "Confirm Troops");
                }
            //We need to make sure the dice is updated according to how many troops that the attacker has.
            } else if(game.getTroopCount(currentAttackCountry) <= attackDiceNumber.getItemCount()) {
                for(int i = 0; i < attackDiceNumber.getItemCount() - game.getTroopCount(currentAttackCountry) + 1; i++) {
                    attackDiceNumber.removeItemAt(i);
                }
            }
        }
    }

    private boolean viableFortifyCountry(String country) {
        return game.getTroopCount(country) != 1 &&
                game.fortifyPossibilities(country).size() != 0;
    }
    private void handleFortifyMouseClick(String country) {
        if(currentFortifyCountry == null) {
            if(viableFortifyCountry(country)) {
                currentFortifyCountry = country;
                repaint();
            }
        } else if(currentFortifiedCountry == null) {
            if(game.fortifyPossibilities(currentFortifyCountry).contains(country)) {
                currentFortifiedCountry = country;
                repaint();
                Integer[] possibleTroops = new Integer[game.getTroopCount(currentFortifyCountry) - 1];
                for(int i = 0; i < possibleTroops.length; i++) {
                    possibleTroops[i] = possibleTroops.length - i;
                }
                optionBox(country, new JPanel(), possibleTroops, "Confirm");
            } else if (viableFortifyCountry(country)) {
                currentFortifyCountry = country;
                repaint();
            } else {
                currentFortifyCountry = null;
            }
        } else if(viableFortifyCountry(country)) {
            clearAndReset(this);
            currentFortifyCountry = country;
        }
    }

    public Point panelLocation(Point countryPoint) {
        return new Point((int) countryPoint.getX(),(int) countryPoint.getY() + (DIAMETER_CIRCLE * 3 / 2));
    }

    private Point middleBottomInfoLocation() {
        int x = getWidth() / 2 - INFO_OFFSET_FROM_MIDDLE;
        int y = getHeight() - OFFSET_LIST_PLAYERS_Y + (playerColors.size() - 1) * OFFSET_INBETWEEN_TEXT;
        return new Point(x,y);
    }

    //Helper method that makes the button to end different phases
    private void endPhaseButton(String text) {
        JButton endPhaseButton = new JButton(text);
        endPhaseButton.setVisible(true);
        endPhaseButton.setSize(endPhaseButton.getPreferredSize());
        endPhaseButton.setLocation(getWidth() / 2,
                (int) middleBottomInfoLocation().getY() - OFFSET_INBETWEEN_TEXT);
        endPhaseButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(game.getCurrentPhase() == Game.Phase.ATTACK && game.getMinimumTroopsDefeatedCountry() == 0) {
                    game.endAttackPhase();
                } else if(game.getCurrentPhase() == Game.Phase.FORTIFY) {
                    game.endFortifyPhase();
                }
                if(game.getMinimumTroopsDefeatedCountry() == 0) {
                    clearAndReset(endPhaseButton.getParent());
                }
            }
        });
        add(endPhaseButton);
        revalidate();
    }
    private void drawPhaseInformation(Graphics g) {
        g.setColor(Color.BLACK);
        if(game.getCurrentPhase() == Game.Phase.DRAFT) {
            g.drawString("Current Reinforcement Troops: " + game.getCurrentReinforcementTroopsNumber(),
                    (int) middleBottomInfoLocation().getX(), (int) middleBottomInfoLocation().getY());
        } else if(game.getCurrentPhase() == Game.Phase.ATTACK) {
            endPhaseButton("End Attack");
        } else if(game.getCurrentPhase() == Game.Phase.FORTIFY) {
            endPhaseButton("End Fortify");
        }
    }

    private void drawCountryTroops(Graphics g) {
        for(String country: countryCoordinates.keySet()) {
            Point coordinates = countryCoordinates.get(country);
            if(country == currentAttackCountry || country == currentDefendCountry ||
                    (currentAttackCountry != null && currentDefendCountry == null
                            && game.getOpposingCountries(currentAttackCountry).contains(country))) {
                g.setColor(country == currentAttackCountry ? ATTACK_COLOR : DEFEND_COLOR);
                for(int i = 0; i < 3; i++) {
                    g.drawOval(coordinates.x - EXTRA_CIRCLE_SIZE + i,
                            coordinates.y - EXTRA_CIRCLE_SIZE + i,
                            DIAMETER_CIRCLE + (EXTRA_CIRCLE_SIZE - i) * 2,
                            DIAMETER_CIRCLE + (EXTRA_CIRCLE_SIZE - i) * 2);
                }

            }
            if (country == currentFortifyCountry || country == currentFortifiedCountry ||
                    (currentFortifyCountry != null && currentFortifiedCountry == null &&
                            game.fortifyPossibilities(currentFortifyCountry).contains(country))) {
                g.setColor(country == currentFortifyCountry ? FORTIFY_COLOR : FORTIFIED_COLOR);
                for(int i = 0; i < 3; i++) {
                    g.drawOval(coordinates.x - EXTRA_CIRCLE_SIZE + i,
                            coordinates.y - EXTRA_CIRCLE_SIZE + i,
                            DIAMETER_CIRCLE + (EXTRA_CIRCLE_SIZE - i) * 2,
                            DIAMETER_CIRCLE + (EXTRA_CIRCLE_SIZE - i) * 2);
                }
            }
            String player = game.getOccupantName(country);
            Color color = playerColors.get(player);
            g.setColor(color);
            g.fillOval(coordinates.x, coordinates.y, DIAMETER_CIRCLE, DIAMETER_CIRCLE);
            g.setColor(Color.WHITE);
            int troops = game.getTroopCount(country);
            g.drawString(troops + "", coordinates.x + (troops < 10 ? X_OFFSET_ONE_DIGIT : X_OFFSET_TWO_DIGITS),
                    coordinates.y + Y_OFFSET);
        }
    }

    private void drawBottomBar(Graphics g) {
        int offsetFromTop = getHeight() - OFFSET_LIST_PLAYERS_Y;
        java.util.List<String> playerNamesInOrder = game.getPlayerOrder();
        for (int i = 0; i < EXTRA_CIRCLE_THICKNESS; i++) {
            String playerName = playerNamesInOrder.get(i);
            g.setColor(Color.BLACK);
            g.drawString("Player " + playerName, OFFSET_LIST_PLAYERS_X, offsetFromTop);
            g.setColor(playerColors.get(playerName));
            g.fillOval(OFFSET_LIST_PLAYERS_X - (OVAL_SPACING * 2), offsetFromTop - OVAL_SPACING, OVAL_SPACING, OVAL_SPACING);
            offsetFromTop += OFFSET_INBETWEEN_TEXT;

        }
        offsetFromTop -= (OFFSET_INBETWEEN_TEXT * 2);
        g.setColor(Color.BLACK);
        g.drawString("Current Phase: " + game.getCurrentPhase().toString(),
                getWidth() - OFFSET_INFO, offsetFromTop);
        offsetFromTop += OFFSET_INBETWEEN_TEXT;
        g.drawString("Current Player: " + game.getCurrentPlayerName(), getWidth() - OFFSET_INFO, offsetFromTop);
        g.setColor(playerColors.get(game.getCurrentPlayerName()));
        offsetFromTop = OFFSET_LIST_CARDS_Y;
        g.drawString("Current Cards: ", OFFSET_LIST_CARDS_X, offsetFromTop);
        for (String card : game.getCards(game.getCurrentPlayerName())) {
            offsetFromTop += OFFSET_INBETWEEN_TEXT;
            String[] cardSplit = card.split("\\(");
            String country = "";
            String cardText = card;

            if(cardSplit.length != 1) {
                country = cardSplit[1];
                country = country.substring(0, country.length() - 1);
                if(game.getOccupantName(country).equals(game.getCurrentPlayerName())) {
                    cardText += "(+2)";
                }
            }
            g.drawString(cardText, OFFSET_LIST_CARDS_X, offsetFromTop);
        }
        if (game.canTurnInCards(game.turnInCards())) {
            turnInCardsButton(offsetFromTop + OFFSET_INBETWEEN_TEXT);
        }
    }

    private void turnInCardsButton(int offsetFromTop) {
        JButton turnInCardsButton = new JButton("Turn in Cards");
        turnInCardsButton.setVisible(true);
        turnInCardsButton.setSize(turnInCardsButton.getPreferredSize());
        turnInCardsButton.setLocation(OFFSET_LIST_CARDS_X, offsetFromTop);
        turnInCardsButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                game.turnInCards(game.turnInCards());
                if(game.getMinimumTroopsDefeatedCountry() == 0) {
                    clearAndReset(turnInCardsButton.getParent());
                } else {
                    turnInCardsButton.getParent().repaint();
                }
            }
        });
        add(turnInCardsButton);
        revalidate();
    }

    private void clearAndReset(Container container) {
        currentAttackCountry = null;
        currentDefendCountry = null;
        currentFortifyCountry = null;
        currentFortifiedCountry = null;

        container.removeAll();
        container.revalidate();
        //This is necessary because components still show up until you call this, even though the component is deleted
        container.repaint();
    }
}
