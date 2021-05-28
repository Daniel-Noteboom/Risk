import java.util.*;
public class Board {

   // Contains all continents in the board
   private Set<Continent> continents;
   
   //Map of each country to their bordering countries
   private Map<Country, Set<Country>> borderingCountries; 
   
   //Maps the name of the country to the country
   private Map<String, Country> countries;

   //The user needs to check for consistency to make sure that the Map of borderin countries is consistent
   //(that is, if country A borders country B, then country B should border country A). The set of continents should not
   //contain any country that is not included in the keyList of the Map of borderingCountries. Every country in the keyList
   //should only be in one and only one continent according to the rules of Risk.
   public Board(Set<Continent> continents, Map<Country, Set<Country>> borderingCountries) {
      this.continents = continents;
      this.borderingCountries = borderingCountries;
      Map<String, Country> countries = new HashMap<String, Country>();
      for(Country c: borderingCountries.keySet()) {
         countries.put(c.getName(), c);
      }
      this.countries = countries;
   }
   
   private Country getCountry(String country) {
      return countries.get(country);
   }
   //Returns whether this board contains this country
   public boolean containsCountry(String country) {
      return borderingCountries.containsKey(getCountry(country));
   }
   
   //Returns whether Country A is bordering Country B
   public boolean isBordering(String countryA, String countryB) {
      return borderingCountries.get(getCountry(countryA)).contains(getCountry(countryB));
   }
   
   public int getTroopCount(String country) {
      return getCountry(country).getTroopCount();
   }
   public Player getOccupant(String country) {
      return getCountry(country).getOccupant();
   }
   
   public void reduceTroops(String country, int troops) {
      getCountry(country).reduceTroops(troops);
   }

   public void increaseTroops(String country, int troops) {
      getCountry(country).increaseTroops(troops);
   }

   public void changeOccupant(String country, Player occupant, int troops) {
      getCountry(country).changeOccupant(occupant, troops);
   }

   //Returns a random Country that is unoccupied.
   //countriesOccupied-The number of countries that are occupied
   //If countriesOccupied is not an accurate number it is possible, but not guaranteed, that a Country with a blank
   //name will be returned
   public Country randomUnoccupiedCountry(int countriesOccupied) {
      int randomCountry =  (int) (Math.random() * (borderingCountries.keySet().size() - countriesOccupied));
      int countriesCovered = 0;
      for (Country c: borderingCountries.keySet()) {
         if (c.getOccupant() == null) {
            if(countriesCovered == randomCountry) {
               return c;
            } else {
               countriesCovered++;
            }
         }
      }
      return new Country("");
   }
   //Returns a randomCountry belonging to Player p
   public String randomCountry(Player p) {
      Set<Country> currentPlayerCountries = countriesOccupied(p);
      int randomCountry = (int) (Math.random() * currentPlayerCountries.size());
      int countriesCovered = 0;
      for(Country c: currentPlayerCountries) {
         if (countriesCovered == randomCountry) {
            return c.getName();
         }
         countriesCovered++;
      }
      return "";
   }


   /**
    * Determines whether the player is done or not. They are done if they no longer control any countries
    * @param player The player that we're seeing is done
    * @return Whether the player is done
    */
   public boolean playerFinished(Player player) {
      for(Country c: borderingCountries.keySet()) {
         if(c.getOccupant() == player) {
            return false;
         }
      }
      return true;
   }

   //Returns the countries currently occupied by this player
   public Set<Country> countriesOccupied(Player player) {
      Set<Country> playerCountries = new HashSet<Country>();
      for(Country c: borderingCountries.keySet()) {
         if(c.getOccupant() == player) {
            playerCountries.add(c);
         }
      }
      return playerCountries;
   }

   /**
    * Finds all the countries that are connected for the given country. Connected means that the occupant can
    * trace a path to these countries without having to enter enemy territory
    * @param countryName The name of the country we're looking for connected countries
    * @return The set of countryNames that are connected
    */
   public Set<String> connectedCountries(String countryName) {
      Country country = getCountry(countryName);
      Set<Country> connectedCountries = new HashSet<Country>();
      Set<Country> countriesCovered = new HashSet<Country>();
      //Add this country in temporarily to find all the connected countries
      connectedCountries.add(country);
      connectedCountries(country, connectedCountries, countriesCovered);
      Set<String> countryNames = new HashSet<>();
      for(Country c: connectedCountries) {
         countryNames.add(c.getName());
      }
      //We need to remove the country itself now!
      countryNames.remove(countryName);
      return countryNames;
   }

   /**
    * Private recursive method that returns all the connected countries for the given country. For definition of
    * what connected means refer to the non-recursive method with less parameters with the same name
    * @param country The country that we're looking for connected countries for
    * @param connectedCountries The countries that are connected together
    * @param countriesCovered The countries that have been covered on the board.
    */
   private void connectedCountries(Country country, Set<Country> connectedCountries,
                                           Set<Country> countriesCovered) {
      //Immediately add the country to covered so that we don't keep hitting it in recursion
      countriesCovered.add(country);
      for(Country borderCountry: borderingCountries.get(country)) {
         if(country.getOccupant() == borderCountry.getOccupant()) {
            //If we haven't covered this country we need to add and call the recursive method to find other countries
            if (!(countriesCovered.contains(borderCountry))) {
               connectedCountries.add(borderCountry);
               connectedCountries(borderCountry, connectedCountries, countriesCovered);
            }
         //Add the countries where the occupant is not correct to covered
         } else {
            countriesCovered.add(borderCountry);
         }
      }
   }

   /**
    * Returns the number of troops a player gets from continent bonuses
    * @param player The player that you're returning troops from continents for
    * @return The number of troops the player will get from controlling entire continent
    */
   public int troopsFromContinents(Player player) {
      int troops = 0;
      for(Continent cont: continents) {
         boolean controlsContinent = true;
         for(Country country: cont.getCountries()) {
            if(country.getOccupant() != player) {
               controlsContinent = false;
            }
         }
         if(controlsContinent) {
            troops += cont.getTroopNumber();
         }
      }
      return troops;
   }
   //Returns the number of countries on the board
   public int numberCountries() {
      return borderingCountries.keySet().size();
   }

   //Clears the troops for the entire board
   public void clearTroops() {
      for(Country c: borderingCountries.keySet()) {
         c.changeOccupant(null, 0);
      }
   }

   /**
    * Returns all countries bordering country with name countryName whose occupant is different than the current
    * country with countryName
    * @param countryName The name of the country
    * @return The countries bordering with different occupants
    */
   public Set<String> opposingCountries(String countryName) {
      Country country = countries.get(countryName);
      Set<Country> borderCountries = borderingCountries.get(country);
      Set<String> opposingCountries = new HashSet<>();
      for(Country c: borderCountries) {
         if(c.getOccupant() != country.getOccupant()) {
            opposingCountries.add(c.getName());
         }
      }
      return opposingCountries;
   }

}