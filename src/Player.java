import java.util.*;
public class Player {
   private String name;
   private List<String> cards;
   private boolean out;

   public Player(String name) {
      this.name = name;
      this.cards = new ArrayList<String>();
      this.out = false;
   }

   public String getName() {
      return name;
   }

   public List<String> getCards() {
      return cards;
   }

   public boolean isOut() {
      return out;
   }

   public void setOut() {
      out = true;
   }

   @Override
   public String toString() {
      return name;
   }
}