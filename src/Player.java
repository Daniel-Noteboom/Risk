import java.util.*;
public class Player {
   private String name;
   private List<Card> cards;
   private boolean out;
   private boolean attackThisTurn;

   public Player(String name) {
      this.name = name;
      this.cards = new ArrayList<Card>();
      this.out = false;
      this.attackThisTurn = false;
   }

   public String getName() {
      return name;
   }

   public List<Card> getCards() {
      return cards;
   }

   public boolean isOut() {
      return out;
   }

   public void setOut() {
      out = true;
   }

   public void setAttackThisTurn() {
      this.attackThisTurn = true;
   }

   public void unSetAttackThisTurn() {
      this.attackThisTurn = false;
   }

   public boolean attackThisTurn() {
      return attackThisTurn;
   }

   public void addCard(Card card) {
      cards.add(card);
   }

   @Override
   public String toString() {
      return name;
   }
}