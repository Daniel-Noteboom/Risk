import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.io.*;
import java.awt.*;
public class Risk {
   private static final Integer[] NUMBER_PLAYERS = {3,4,5,6};
   private static final Color[] POSSIBLE_PLAYER_COLORS = {Color.BLACK, Color.GRAY, Color.BLUE,
           Color.decode("#800080") /*purple*/, Color.decode("#800000") /*Maroon*/, Color.decode("#00A36C") /*Green*/};
   public static void main(String[] args) {
      JFrame f = new JFrame("Risk Game");
      JPanel panel = new JPanel(new BorderLayout(5, 10));
      JLabel label = new JLabel("Risk", SwingConstants.CENTER);
      label.setFont(new Font(label.getFont().getName(), Font.PLAIN, 20));
      panel.add(label, BorderLayout.NORTH);

      JComboBox<Integer> playerNumbers = new JComboBox<>(NUMBER_PLAYERS);
      panel.add(playerNumbers, BorderLayout.CENTER);
      JButton button = new JButton("Start Game");
      button.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            f.remove(panel);
            String[] playerNames = new String[(int) playerNumbers.getSelectedItem()];
            for(int i = 0; i < playerNames.length; i++) {
               playerNames[i] = "" + (i + 1);
            }
            Game game = createGame(playerNames, (int) (Math.random() * playerNames.length),
                    "risk_data.txt", true, true);
            RiskView mainView = game.getRiskViews().get(0);
            mainView.addMouseListener(new MouseAdapter() {
               public void mouseClicked(MouseEvent e) {
                  mainView.mouseClicked(e.getX(), e.getY());
               }
            });
            f.add(mainView);
            f.setSize(mainView.getWidth(), mainView.getHeight() +
                    mainView.getExtraBottomSpace());
            f.setLocationRelativeTo(null);
         }
      });
      panel.add(button, BorderLayout.SOUTH);
      panel.add(new JLabel("Choose Player Number"), BorderLayout.WEST);
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.add(panel);
      f.setLocationRelativeTo(null);
      f.setSize(230, 230);
      f.setVisible(true);
   }
   
   public static Game createGame(String[] playerNames, int startingPlayer, String fileName, boolean useView, boolean randomlyPopulate) {
      Scanner sc = new Scanner("");
      try {
         sc = new Scanner(new File(fileName));
         
      } catch(FileNotFoundException e) {
         System.err.println("You need to include a valid file name");
      }
      String picFile = sc.nextLine().split(":")[1].trim();
      boolean moreContinents = true;
      int countryCount = 0;
      Set<Continent> continents = new HashSet<>();
      Map<Country, Set<Country>> borderingCountries = new HashMap<>();
      Map<String, Country> nameToCountry = new HashMap<>();
      while(sc.hasNextLine() && moreContinents) {
         String continentLine = sc.nextLine();
         if (!continentLine.trim().equals("EndContinents")) {
            String[] continentContents = continentLine.split(":");
            String[] countriesInContinent = sc.nextLine().split(":")[1].split(",");
            int continentValue = Integer.parseInt(sc.nextLine().split(":")[1].trim());
            
            Set<Country> countries = new HashSet<>();
            countryCount += countriesInContinent.length;

            for (String s : countriesInContinent) {
               String countryName = s.trim();
               Country country = new Country(countryName, 0, null);
               countries.add(country);
               nameToCountry.put(countryName, country);
               borderingCountries.put(country, new HashSet<>());
            }
            Continent continent = new Continent(continentValue, countries, continentContents[1].trim());
            continents.add(continent);
         } else {
           moreContinents = false;
         }
      }
      int secondCountryCount = 0;
      Map<String, Point> countryCoordinates = new HashMap<>();
      while(sc.hasNextLine()) {
           secondCountryCount++;
           String countryName = sc.nextLine().split(":")[1].trim();
           if(!nameToCountry.containsKey(countryName)) {
              return null;
           } 
           Country country = nameToCountry.get(countryName);
           String[] borderingCountriesNames = sc.nextLine().split(":")[1].trim().split(",");
           for(String borderingCountryName: borderingCountriesNames) {
              if(!nameToCountry.containsKey(borderingCountryName.trim())) {
                 System.err.println("Country listed that was not found in continent");
                 return null;
              }
              Country borderingCountry = nameToCountry.get(borderingCountryName.trim());
              borderingCountries.get(country).add(borderingCountry);
           }
           String[] coordinates = sc.nextLine().split(":")[1].trim().split(",");
           Point p = new Point(Integer.parseInt(coordinates[0].trim()), Integer.parseInt(coordinates[1].trim()));
           countryCoordinates.put(country.getName(), p);

      } 
      if(countryCount != secondCountryCount) {
         System.err.println("Not every country listed that was in continent");
         return null;
      }
      Board b = new Board(continents, borderingCountries);
      Player[] players = new Player[playerNames.length];
      for(int i = 0; i < playerNames.length; i++) {
        players[i] = new Player(playerNames[i]);
      }
      for (Country c: borderingCountries.keySet()) {
         for(Country borderingC: borderingCountries.get(c)) {
            if(!borderingCountries.get(borderingC).contains(c)) {
               System.err.println(borderingC + " is bordering " + c + " but " + c + " is not bordering " + borderingC);
               return null;
            }
         }
      }
      Map<String, Color> playerColors = new HashMap<>();
      for(int i = 0; i < playerNames.length; i++) {
         playerColors.put(playerNames[i], POSSIBLE_PLAYER_COLORS[i]);
      }
      Game g;
      if(useView) {
         RiskController rC = new RiskController();
         g = new Game(b, players, startingPlayer, false, rC);
         if (randomlyPopulate) {
            g.randomlyPopulateBoard();
         }
         RiskView rV = new RiskView(picFile, countryCoordinates, playerColors, g);
         rC.addRiskView(rV);
      } else {
         g = new Game(b, players, startingPlayer);
         if (randomlyPopulate) {
            g.randomlyPopulateBoard();
         }
      }
      return g;
   }
}