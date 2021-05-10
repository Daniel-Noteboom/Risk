import java.util.*;

public class Continent {
   private int troopNumber;
   private Set<Country> countries;
   private String name;
   
   public Continent(int troopNumber, Set<Country> countries, String name) {
      this.troopNumber = troopNumber;
      this.countries = Collections.unmodifiableSet(countries);
      this.name = name;
   }

   public Set<Country> getCountries() {
      return countries;
   }
   public int getTroopNumber() {
      return troopNumber;
   }
   
   //Returns true if the countries passed in contains all of this classes countries
   public boolean containsAll(Set<Country> countries) {
      return countries.containsAll(this.countries);
   }
   
   public String getName() {
      return name;
   }
   
}