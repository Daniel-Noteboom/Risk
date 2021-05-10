import java.util.*;
public class Country {
   private String name; //current name
   private int troops; //current level of troops
   private Player occupant; //current occupant
   
   // Create a country with a given amount of troops and occupant
   
   //Create Country give name. Sets troops to 0 and player to null
   public Country(String name) {
      this(name, 0, null);
   }
   
   //Create country given a name, troops, and occupant
   public Country(String name, int troops, Player occupant) {
      this.name = name;
      this.troops = troops;
      this.occupant = occupant;
   }
   
   // Return the current troop number in the country
   public int getTroopCount() {
      return troops;
   }
   
   // Return the current occupant of the country
   public Player getOccupant() {
      return occupant;
   }
   
   // Return the current name of the country
   public String getName() {
      return name;
   }
   
   //Reduce the number of troops by reduceTroopNumber
   public void reduceTroops(int reduceTroopNumber) {
      this.troops = this.troops - reduceTroopNumber;
   }
   
   // Increase the troops by IncreaseTroopNumber
   public void increaseTroops(int increaseTroopNumber) {
      this.troops = this.troops + increaseTroopNumber;
   }
   
   // change current occupant and troops to occupant and troops
   public void changeOccupant(Player occupant, int troops) {
      this.occupant = occupant;
      this.troops = troops;
   }
   
   @Override
   public String toString() {
      return name;
   }
}