import java.util.*;
import java.io.*;
class Risk {
   public static void main(String[] args) {
      System.out.println("How many players do you want to play with? Please " +
         "type in a number from three to six in numberical form (3,4,5,6)");
      int numberPlayers = Risk.getNumberPlayers();
      while (numberPlayers == 0) {
         numberPlayers = Risk.getNumberPlayers();
      }
      System.out.println("You will be playing with " + numberPlayers + " players");
      int startingPlayer = (int) (Math.random() * numberPlayers);
      System.out.println("The starting player will be player" + (startingPlayer + 1));
      String[] playerNames = new String[numberPlayers];
      for(int i = 0; i < playerNames.length; i++) {
         playerNames[i] = "" + (i + 1);
      }
      Game game = createGame(playerNames, startingPlayer, "risk_data.txt");
   }
   /* Get the number of players for the risk game. If the player 
   does not give a number they will be prompted to give a number.
   If they give a number that is not in the range of 3-6 then 0
   is returned */
   private static int getNumberPlayers() {
      Scanner scanner = new Scanner(System.in);
      while (!scanner.hasNextInt()) {
         scanner.next();
         System.out.println("Please type in a number from three to six");
      }
      int numberPlayers = scanner.nextInt();
      if (numberPlayers < 3 || numberPlayers > 6) {
         System.out.println("Please type in a number from three to six");
         return 0;
      } else {
         return numberPlayers;
      }
   }
   
   public static Game createGame(String[] playerNames, int startingPlayer, String fileName) {
      Scanner sc = new Scanner("");
      try {
         sc = new Scanner(new File(fileName));
         
      } catch(FileNotFoundException e) {
         System.err.println("You need to include a valid file name");
      }
      
      boolean moreContinents = true;
      int countryCount = 0;
      Set<Continent> continents = new HashSet<Continent>();
      Map<Country, Set<Country>> borderingCountries = new HashMap<Country, Set<Country>>();
      Map<String, Country> nameToCountry = new HashMap<String, Country>();
      while(sc.hasNextLine() && moreContinents) {
         String continentLine = sc.nextLine();
         if (!continentLine.trim().equals("EndContinents")) {
            String[] continentContents = continentLine.split(":");
            String[] countriesInContinent = sc.nextLine().split(":")[1].split(",");
            int continentValue = Integer.parseInt(sc.nextLine().split(":")[1].trim());
            
            Set<Country> countries = new HashSet<Country>();
            countryCount += countriesInContinent.length;
            
            for(int i = 0; i < countriesInContinent.length; i++) {
               String countryName = countriesInContinent[i].trim();
               Country country = new Country(countryName, 0, null);
               countries.add(country);
               nameToCountry.put(countryName, country);
               borderingCountries.put(country, new HashSet<Country>());
            }
            Continent continent = new Continent(continentValue, countries, continentContents[1].trim());
            continents.add(continent);
         } else {
           moreContinents = false;
         }
      }
      int secondCountryCount = 0;
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
      } 
      if(countryCount != secondCountryCount) {
         System.err.println("Not every country listed that was in continent");
         return null;
      }
      Board b = new Board(continents, borderingCountries);
      Player[] players = new Player[playerNames.length];
      Set<String> names = new HashSet<String>();
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
      return new Game(b, players, startingPlayer);
   }
}