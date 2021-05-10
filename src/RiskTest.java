import org.junit.*;
import java.util.*;

public class RiskTest {
    private Game g;
    private Game riskGame;

    /**
     * Fixture initialization (common initialization
     * for all tests).
     **/
    @Before
    public void setUp() {

        Player playerOne = new Player("one");
        Player playerTwo = new Player("two");
        Player playerThree = new Player("three");
        Player playerFour = new Player("four");
        Player playerFive = new Player("five");
        Country countryA = new Country("a", 4, playerOne);
        Country countryB = new Country("b", 4, playerTwo);
        Country countryC = new Country("c", 3, playerFour);
        Country countryD = new Country("d", 4, playerThree);
        Country countryE = new Country("e", 1, playerFour);
        Country countryE2 = new Country("e2", 1, playerFour);
        Country countryF = new Country("f", 2, playerFour);
        Country countryG = new Country("g", 5, playerFive);

        Set<Country> countryAB = new HashSet<>();
        countryAB.add(countryA);
        countryAB.add(countryB);
        Set<Country> countryCD = new HashSet<>();
        countryCD.add(countryC);
        countryCD.add(countryD);
        Set<Country> countryEF = new HashSet<>();
        countryEF.add(countryE);
        countryEF.add(countryF);
        countryEF.add(countryE2);

        Set<Country> setCountryG = new HashSet<>();
        setCountryG.add(countryG);

        Continent continentAB = new Continent(4, countryAB, "ab");
        Continent continentCD = new Continent(4, countryCD, "cd");
        Continent continentEF = new Continent(1, countryEF, "ef");
        Continent continentG = new Continent(1, setCountryG, "g");


        Set<Continent> continents = new HashSet<>();
        continents.add(continentAB);
        continents.add(continentCD);
        continents.add(continentEF);
        continents.add(continentG);

        Map<Country, Set<Country>> borderingCountries = new HashMap<>();
        Set<Country> abce = new HashSet<>();
        Set<Country> bcd = new HashSet<>();
        Set<Country> acd = new HashSet<>();
        Set<Country> abd = new HashSet<>();
        Set<Country> e = new HashSet<>();
        Set<Country> de2f = new HashSet<>();
        Set<Country> eg = new HashSet<>();
        Set<Country> f = new HashSet<>();

        e.add(countryE);

        abce.add(countryA);
        abce.add(countryB);
        abce.add(countryC);
        abce.add(countryE);

        abd.add(countryA);
        abd.add(countryB);
        abd.add(countryD);

        acd.add(countryA);
        acd.add(countryC);
        acd.add(countryD);

        bcd.add(countryB);
        bcd.add(countryC);
        bcd.add(countryD);

        e.add(countryE);

        de2f.add(countryD);
        de2f.add(countryF);
        de2f.add(countryE2);

        eg.add(countryE);
        eg.add(countryG);

        f.add(countryF);

        borderingCountries.put(countryA, bcd);
        borderingCountries.put(countryB, acd);
        borderingCountries.put(countryC, abd);
        borderingCountries.put(countryD, abce);
        borderingCountries.put(countryE, de2f);
        borderingCountries.put(countryF, eg);
        borderingCountries.put(countryE2, e);
        borderingCountries.put(countryG, f);

        Board b = new Board(continents, borderingCountries);
        Player[] players = {playerOne, playerTwo, playerThree, playerFour, playerFive};
        g = new Game(b, players, 0, true);
        String[] playerNames = {"one", "two", "three"};
        riskGame = Risk.createGame(playerNames, 0, "risk_data.txt");
    }


    /**
     * Tests that the dice are working properly
     **/
    @Test
    public void testDice() {
        for (int i = 0; i < 100; i++) {
            List<Integer> dice = Game.rollDice(3);
            Assert.assertEquals("Make sure correct amount of dice being returned", dice.size(), 3);
            Assert.assertTrue("Make sure dice returned are correct numbers", ((dice.get(0) > 0) && (dice.get(0) < Game.DICE_SIDES + 1) &&
                    (dice.get(1) > 0 && dice.get(1) < Game.DICE_SIDES + 1) && (dice.get(2) > 0 && dice.get(2) < Game.DICE_SIDES + 1)));
        }
    }

    /**
     * Tests that the attack function is working correctly
     **/
    @Test
    public void testAttack() {
        g.changeGameState("one", Game.Phase.ATTACK);
        List<Integer> attackDice = new ArrayList<>();
        List<Integer> defendDice = new ArrayList<>();
        attackDice.add(6);
        attackDice.add(5);
        attackDice.add(2);
        defendDice.add(5);
        defendDice.add(4);
        Assert.assertTrue("Make sure the attack worked", g.attack("a", attackDice, "b", defendDice));
        Assert.assertEquals("Make sure correct number of troops for Country a", g.getTroopCount("a"), 4);
        Assert.assertEquals("Make sure correct number of troops for Country b", g.getTroopCount("b"), 2);
        attackDice.set(0, 4);

        Assert.assertTrue("Make sure the attack worked", g.attack("a", attackDice, "b", defendDice));
        Assert.assertEquals("Make sure correct number of troops for Country a", g.getTroopCount("a"), 2);
        Assert.assertEquals("Make sure correct number of troops for Country b", g.getTroopCount("b"), 2);
        attackDice.set(0, 5);
        attackDice.remove(2);
        attackDice.remove(1);
        Assert.assertTrue("Make sure the attack worked", g.attack("a", attackDice, "b", defendDice));
        Assert.assertEquals("Make sure correct number of troops for Country a", g.getTroopCount("a"), 1);
        Assert.assertEquals("Make sure correct number of troops for Country b", g.getTroopCount("b"), 2);
    }

    /**
     * Tests that the attack fails when it should
     **/
    @Test
    public void testAttackFails() {
        g.changeGameState("one", Game.Phase.ATTACK);
        List<Integer> attackDice = new ArrayList<>();
        List<Integer> defendDice = new ArrayList<>();
        attackDice.add(6);
        attackDice.add(5);
        attackDice.add(2);
        attackDice.add(3);
        defendDice.add(5);
        defendDice.add(4);
        g.changeGameState("one", Game.Phase.ATTACK);

        Assert.assertFalse("Make sure the attack fails (too many attack dice)", g.attack("a", attackDice, "b", defendDice));
        attackDice.remove(2);
        g.changeGameState("one", Game.Phase.DRAFT);
        Assert.assertFalse("Make sure the attack fails (wrong phase)", g.attack("a", attackDice, "b", defendDice));
        g.changeGameState("one", Game.Phase.ATTACK);
        defendDice.add(3);
        Assert.assertFalse("Make sure the attack fails (too many defense dice)", g.attack("a", attackDice, "b", defendDice));
        defendDice.remove(2);
        defendDice.remove(1);
        Assert.assertFalse("Make sure the attack fails (not enough defense dice)", g.attack("a", attackDice, "b", defendDice));
        defendDice.remove(0);
        Assert.assertFalse("Make sure the attack fails (not enough defense dice)", g.attack("a", attackDice, "b", defendDice));
        defendDice.add(1);
        defendDice.add(2);
        g.changeGameState("four", Game.Phase.ATTACK);
        Assert.assertFalse("Make sure the attack fails (Not enough troops to attack", g.attack("e", attackDice, "d", defendDice));

        Assert.assertFalse("Make sure the attack fails (Too many attack dice", g.attack("c", attackDice, "d", defendDice));
        attackDice.remove(2);
        attackDice.remove(1);
        attackDice.remove(0);
        g.changeGameState("one", Game.Phase.ATTACK);

        Assert.assertFalse("Make sure the attack fails (not enough attack dice)", g.attack("a", attackDice, "b", defendDice));
        attackDice.add(1);
        g.changeGameState("four", Game.Phase.ATTACK);

        Assert.assertFalse("Make sure the attack fails (can't attack yourself)", g.attack("f", attackDice, "e", defendDice));
        g.changeGameState("three", Game.Phase.ATTACK);
        Assert.assertFalse("Make sure the attack fails (not bordering)", g.attack("d", attackDice, "f", defendDice));

    }

    @Test
    public void testRandomlyPopulate() {
        g.randomlyPopulateBoard();
        Map<String, Integer> playerToTroops = new HashMap<>();
        String[] countries = {"a", "b", "c", "d", "e", "e2", "f", "g"};
        for (String country: countries) {
            String player = g.getOccupantName(country);
            int troops = g.getTroopCount(country);
            if (playerToTroops.containsKey(player)) {
                playerToTroops.put(player, troops + playerToTroops.get(player));
            } else {
                playerToTroops.put(player, troops);
            }
        }
        int troopNumber = playerToTroops.get("one");
        Assert.assertNotEquals("Make sure troops is not zero", 0, troopNumber);
        String[] otherNames = {"two", "three", "four", "five"};
        for (String player : otherNames) {
            Assert.assertEquals("Make sure all the players are present and have the same number of troops", troopNumber, (int) playerToTroops.get(player));
        }
        Assert.assertEquals("Make sure player is correct", g.getCurrentPlayerName(), "one");
        Assert.assertEquals("Make sure the current phase is correct", Game.Phase.DRAFT, g.getCurrentPhase());
    }

    @Test
    public void testDraftPhase() {
        g.changeGameState("one", Game.Phase.DRAFT);
        Assert.assertEquals("Make sure the number of troops in the draft is correct",
                3, g.getCurrentReinforcementTroopsNumber());
        Assert.assertTrue(g.reinforceTroops(3, "a"));
        Assert.assertEquals("Make sure it actually added troops", 7, g.getTroopCount("a"));

        g.changeGameState("four", Game.Phase.DRAFT);
        Assert.assertEquals("Make sure the number of troops in the draft is correct",
                5, g.getCurrentReinforcementTroopsNumber());

        g.changeGameState("five", Game.Phase.DRAFT);
        Assert.assertEquals("Make sure the number of troops in the draft is correct",
                6, g.getCurrentReinforcementTroopsNumber());
        riskGame.randomlyPopulateBoard();

        Assert.assertTrue("Make sure the number of troops in draft is correct", riskGame.getCurrentReinforcementTroopsNumber() >= 4);
        riskGame.changeGameState("three", Game.Phase.DRAFT);
        Assert.assertTrue("Make sure the number of troops in draft is correct", riskGame.getCurrentReinforcementTroopsNumber() >= 5);
        Set<String> countryNames = riskGame.getCountries("three");
        Iterator<String> iterator = countryNames.iterator();
        String sampleCountry = iterator.next();
        Assert.assertTrue("Make sure we can add the correct amount of troops", riskGame.reinforceTroops(5, sampleCountry));
        riskGame.changeGameState("three", Game.Phase.DRAFT);
        sampleCountry = iterator.next();
        Assert.assertTrue("Make sure we can add the correct amount of troops", riskGame.reinforceTroops(4, sampleCountry));
        sampleCountry = iterator.next();
        Assert.assertTrue("Make sure we can add the correct amount of troops", riskGame.reinforceTroops(1, sampleCountry));
    }

    @Test
    public void testDraftPhaseFails() {
        g.changeGameState("one", Game.Phase.DRAFT);
        Assert.assertFalse("Too many troops used in draft", g.reinforceTroops(4, "a"));
        Assert.assertEquals("Make sure no troops were added", 4, g.getTroopCount("a"));
        g.reinforceTroops(2, "a");
        Assert.assertFalse("Too many troops used in draft", g.reinforceTroops(2, "a"));
        Assert.assertEquals("Make sure no troops were added", 6, g.getTroopCount("a"));

        Assert.assertFalse("Wrong country to reinforce with", g.reinforceTroops(1, "b"));
        Assert.assertEquals("Make sure no troops were added", 4, g.getTroopCount("b"));

        g.reinforceTroops(1, "a");
        Assert.assertFalse("Wrong phase to draft in", g.reinforceTroops(1, "b"));
        Assert.assertEquals("Make sure no troops were added", 4, g.getTroopCount("b"));

    }

    @Test
    public void testFortifyPhase() {
        g.changeGameState("four", Game.Phase.FORTIFY);
        Set<String> fortifyPossibilities = g.fortifyPossibilities("f");
        Assert.assertEquals("Make sure only two possibilities are returned", 2, fortifyPossibilities.size());
        for(String country: fortifyPossibilities) {
            Assert.assertTrue("Make sure the possibility is correct", country.contains("e"));
        }
        Assert.assertTrue("Make sure the fortify does not have errors", g.fortifyTroops("f", "e2", 1));
        Assert.assertEquals("Make sure the country gained troops to fortification", 2, g.getTroopCount("e2"));
        Assert.assertEquals("Make sure the country lost troops to fortification", 1, g.getTroopCount("f"));
    }

    @Test
    public void testFortifyPhaseFails() {
        g.changeGameState("four", Game.Phase.FORTIFY);
        Assert.assertFalse("Not enough troops", g.fortifyTroops("f", "e2", 2));
        Assert.assertEquals("Make sure f still has two troops", 2, g.getTroopCount("f"));
        Assert.assertEquals("Make sure e2 still has one troop", 1, g.getTroopCount("e2"));

        Assert.assertFalse("Not bordering", g.fortifyTroops("f", "c", 1));
        Assert.assertEquals("Make sure f still has two troops", 2, g.getTroopCount("f"));
        Assert.assertEquals("Make sure c still has three troops", 3, g.getTroopCount("c"));

        Assert.assertFalse("Not controlled by right person", g.fortifyTroops("f", "d", 1));
        Assert.assertEquals("Make sure f still has two troops", 2, g.getTroopCount("f"));
        Assert.assertEquals("Make sure d still has four troops", 4, g.getTroopCount("d"));

        g.fortifyTroops("f", "e", 1);
        Assert.assertFalse("wrong phase", g.fortifyTroops("e", "f", 1));
        Assert.assertEquals("Make sure e has two troops", 2, g.getTroopCount("e"));
        Assert.assertEquals("Make sure f has one troop", 1, g.getTroopCount("f"));

        g.changeGameState("three", Game.Phase.FORTIFY);
        Assert.assertFalse("wrong phase", g.fortifyTroops("e", "f", 1));
        Assert.assertEquals("Make sure e has two troops", 2, g.getTroopCount("e"));
        Assert.assertEquals("Make sure f has one troop", 1, g.getTroopCount("f"));

    }

    @Test
    public void testSetTroopsDefeatedCountry() {
        g.changeGameState("four", Game.Phase.ATTACK);
        List<Integer> attackDice = new ArrayList<>();
        attackDice.add(6);
        attackDice.add(6);
        List<Integer> defendDice = new ArrayList<>();
        defendDice.add(5);
        defendDice.add(5);
        Assert.assertTrue("Make sure attack worked", g.attack("c", attackDice, "b", defendDice));
        Assert.assertTrue("Make sure attack worked", g.attack("c", attackDice, "b", defendDice));
        Assert.assertTrue("Make sure we can set the troops in the defeated country", g.setTroopsDefeatedCountry(2));
        Assert.assertEquals(g.getOccupantName("b"), "four");
        Assert.assertEquals(g.getOccupantName("c"), "four");
        Assert.assertEquals(g.getTroopCount("b"), 2);
        Assert.assertEquals(g.getTroopCount("c"), 1);
        g.changeGameState("one", Game.Phase.ATTACK);
        Assert.assertTrue("Make sure attack worked", g.attack("a", attackDice, "b", defendDice));
        Assert.assertTrue("Make sure we can set the troops in the defeated country", g.setTroopsDefeatedCountry(3));
        Assert.assertEquals(g.getOccupantName("a"), "one");
        Assert.assertEquals(g.getOccupantName("b"), "one");
        Assert.assertEquals(g.getTroopCount("a"), 1);
        Assert.assertEquals(g.getTroopCount("b"), 3);

        g.changeGameState("five", Game.Phase.ATTACK);
        attackDice.add(6);
        Assert.assertTrue("Make sure attack worked", g.attack("g", attackDice, "f", defendDice));
        Assert.assertTrue("Make sure we can set the number of troops in the defeated country", g.setTroopsDefeatedCountry(4));
        Assert.assertEquals(g.getTroopCount("f"), 4);
        Assert.assertEquals(g.getTroopCount("g"), 1);

    }

    @Test
    public void testSetTroopsDefeatedCountryFails() {
        g.changeGameState("four", Game.Phase.ATTACK);
        List<Integer> attackDice = new ArrayList<>();
        attackDice.add(6);
        attackDice.add(6);
        List<Integer> defendDice = new ArrayList<>();
        defendDice.add(5);
        defendDice.add(5);
        Assert.assertTrue("Make sure attack worked", g.attack("c", attackDice, "b", defendDice));
        Assert.assertFalse("Make sure we can't set the wrong number of troops in a non-defeated country", g.setTroopsDefeatedCountry(2));
        Assert.assertEquals(g.getTroopCount("b"), 2);
        Assert.assertEquals(g.getTroopCount("c"), 3);
        Assert.assertTrue("Make sure attack worked", g.attack("c", attackDice, "b", defendDice));
        Assert.assertFalse("Make sure we can't set the wrong number of troops in the defeated country", g.setTroopsDefeatedCountry(1));
        Assert.assertFalse("Make sure we can't set the wrong number of troops in the defeated country", g.setTroopsDefeatedCountry(3));
        Assert.assertEquals(g.getTroopCount("b"), 0);
        Assert.assertEquals(g.getTroopCount("c"), 3);

        g.changeGameState("five", Game.Phase.ATTACK);
        attackDice.add(6);
        Assert.assertTrue("Make sure attack worked", g.attack("g", attackDice, "f", defendDice));

        Assert.assertFalse("Make sure we can't set the wrong number of troops in the defeated country", g.setTroopsDefeatedCountry(1));
        Assert.assertFalse("Make sure we can't set the wrong number of troops in the defeated country", g.setTroopsDefeatedCountry(2));
        Assert.assertFalse("Make sure we can't set the wrong number of troops in the defeated country", g.setTroopsDefeatedCountry(5));
        Assert.assertEquals(g.getTroopCount("f"), 0);
        Assert.assertEquals(g.getTroopCount("g"), 5);

    }

    /**
     *         Country countryA = new Country("a", 4, playerOne);
     *         Country countryB = new Country("b", 4, playerTwo);
     *         Country countryC = new Country("c", 3, playerFour);
     *         Country countryD = new Country("d", 4, playerThree);
     *         Country countryE = new Country("e", 1, playerFour);
     *         Country countryE2 = new Country("e2", 1, playerFour);
     *         Country countryF = new Country("f", 2, playerFour);
     *         Country countryG = new Country("g", 5, playerFive);
     */
    @Test
    public void testFullGameFlow() {
        Assert.assertTrue(g.reinforceTroops(3, "a")); //A: 7 troops
        Assert.assertEquals("make sure it is now attack phase", Game.Phase.ATTACK, g.getCurrentPhase());

        List<Integer> attackDice = new ArrayList<>();
        List<Integer> defendDice = new ArrayList<>();
        attackDice.add(6);
        attackDice.add(6);
        attackDice.add(6);
        defendDice.add(6);
        defendDice.add(6);
        Assert.assertTrue(g.attack("a", attackDice, "b", defendDice)); //A: 5 troops, B: 4 troops
        Assert.assertTrue(g.attack("a", attackDice, "b", defendDice)); //A: 3 troops, B: 4 troops
        defendDice.set(1, 5);
        defendDice.set(0, 5);
        attackDice.remove(2);
        Assert.assertTrue(g.attack("a", attackDice, "b", defendDice)); //A: 3 troops, B: 2 troops
        Assert.assertEquals(g.getTroopCount("a"), 3);
        Assert.assertEquals(g.getTroopCount("b"), 2);
        attackDice.add(6);
        g.endAttackPhase();
        Assert.assertEquals("make sure it is now fortify phase", Game.Phase.FORTIFY, g.getCurrentPhase());
        g.skipFortifyPhase();
        Assert.assertEquals("make sure it is now draft phase", Game.Phase.DRAFT, g.getCurrentPhase());
        Assert.assertTrue(g.reinforceTroops(3, "b"));
        Assert.assertEquals(g.getTroopCount("b"), 5);

        Assert.assertTrue(g.attack("b", attackDice, "c", defendDice)); //B: 5 troops, C: 2 troops
        defendDice.remove(1);
        Assert.assertTrue(g.attack("b", attackDice, "c", defendDice)); //B: 5 troops, C: 0 troops
        Assert.assertTrue(g.setTroopsDefeatedCountry(3));
        Assert.assertEquals(g.getTroopCount("b"), 2);
        Assert.assertEquals(g.getTroopCount("c"), 3);
        Assert.assertEquals(g.getOccupantName("c"), "two");
        Assert.assertEquals(g.getOccupantName("b"), "two");
        Assert.assertEquals(g.getOccupantName("a"), "one");
        defendDice.add(5);
        attackDice.remove(2);
        Assert.assertTrue(g.attack("c", attackDice, "d", defendDice)); //C: 3 troops, D: 2 troops
        Assert.assertTrue(g.attack("c", attackDice, "d", defendDice)); //C: 3 troops, D: 0 troops
        Assert.assertTrue(g.setTroopsDefeatedCountry(2));
        Assert.assertEquals(g.getTroopCount("c"), 1);
        Assert.assertEquals(g.getTroopCount("d"), 2);
        Assert.assertEquals(g.getOccupantName("c"), "two");
        Assert.assertEquals(g.getOccupantName("d"), "two");
        attackDice.add(6);
        g.endAttackPhase();
        Assert.assertEquals("Make sure fortify possibilities are correct", g.fortifyPossibilities("d").size(), 2);
        Assert.assertTrue(g.fortifyTroops("b", "d", 1));
        Assert.assertEquals(g.getTroopCount("b"), 1);
        Assert.assertEquals(g.getTroopCount("d"), 3);
        Assert.assertTrue(g.reinforceTroops(5, "e"));
        Assert.assertEquals(g.getTroopCount("e"), 6);
        g.endAttackPhase();
        g.skipFortifyPhase();
        Assert.assertTrue(g.reinforceTroops(6, "g"));
        Assert.assertEquals(g.getTroopCount("g"), 11);
        g.endAttackPhase();
        g.skipFortifyPhase();
        Assert.assertTrue(g.reinforceTroops(3, "a"));
        Assert.assertEquals(g.getTroopCount("a"), 6);
        g.endAttackPhase();
        g.skipFortifyPhase();
        Assert.assertTrue(g.reinforceTroops(7, "d"));
        Assert.assertEquals(g.getTroopCount("d"), 10);
        Assert.assertTrue(g.attack("d", attackDice, "e", defendDice)); //D: 10 troops, E: 4 troops
        Assert.assertTrue(g.attack("d", attackDice, "e", defendDice)); //E: 2 troops
        Assert.assertTrue(g.attack("d", attackDice, "e", defendDice)); //E: 0 troops
        Assert.assertTrue(g.setTroopsDefeatedCountry(7));
        Assert.assertEquals(g.getTroopCount("d"), 3);
        Assert.assertEquals(g.getTroopCount("e"), 7);
        Assert.assertEquals(g.getOccupantName("d"), "two");
        Assert.assertEquals(g.getOccupantName("e"), "two");
        defendDice.remove(1);
        Assert.assertTrue(g.attack("e", attackDice, "e2", defendDice));
        Assert.assertTrue(g.setTroopsDefeatedCountry(3));
        Assert.assertEquals(g.getTroopCount("e"), 4);
        Assert.assertEquals(g.getTroopCount("e2"), 3);
        Assert.assertEquals(g.getOccupantName("e2"), "two");
        Assert.assertEquals(g.getOccupantName("e"), "two");

        defendDice.add(5);
        g.attack("e", attackDice, "f", defendDice);
        Assert.assertTrue(g.setTroopsDefeatedCountry(3));
        Assert.assertEquals(g.getTroopCount("e"), 1);
        Assert.assertEquals(g.getTroopCount("f"), 3);
        Assert.assertEquals(g.getOccupantName("e"), "two");
        Assert.assertEquals(g.getOccupantName("f"), "two");
        g.endAttackPhase();
        g.fortifyTroops("e2", "f", 2);
        Assert.assertEquals(g.getTroopCount("f"), 5);
        Assert.assertTrue(g.reinforceTroops(4, "g"));
        Assert.assertEquals(g.getTroopCount("g"), 15);
        g.endAttackPhase();
        g.skipFortifyPhase();
        g.reinforceTroops(3, "a");
        Assert.assertEquals(g.getTroopCount("a"), 9);
        g.endAttackPhase();
        g.skipFortifyPhase();
        Assert.assertTrue(g.reinforceTroops(8, "f"));
        Assert.assertEquals(g.getTroopCount("f"), 13);
        for(int i = 0; i < 7; i++) {
            Assert.assertTrue(g.attack("f", attackDice, "g", defendDice));
        }
        defendDice.remove(1);
        g.attack("f", attackDice, "g", defendDice);
        g.setTroopsDefeatedCountry(12);
        Assert.assertEquals(g.getTroopCount("f"), 1);
        Assert.assertEquals(g.getTroopCount("g"), 12);
        Assert.assertEquals(g.getOccupantName("f"), "two");
        Assert.assertEquals(g.getOccupantName("g"), "two");
        g.endAttackPhase();
        g.skipFortifyPhase();
        Assert.assertTrue(g.reinforceTroops(3, "a"));
        Assert.assertEquals(g.getTroopCount("a"), 12);
        g.endAttackPhase();
        g.skipFortifyPhase();
        Assert.assertTrue(g.reinforceTroops(9, "b"));
        defendDice.add(5);
        for(int i = 0; i < 6; i++) {
            Assert.assertTrue(g.attack("b", attackDice, "a", defendDice));
        }
        Assert.assertEquals(g.getCurrentPhase(), Game.Phase.ENDGAME);






    }
}


