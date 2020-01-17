class GameCharacter {
	
	private int health;

	/**
	 * Initializes this game character with the given health.
	 * @param health
	 * @pre The given health is nonnegative.
	 *     | 0 <= health
	 */
	public GameCharacter(int health) {
		this.health = health;
	}

	public int getHealth() { return this.health; }

	/**
	 * Reduces this game character's health by the given amount.
	 * @pre The given amount is nonnegative.
	 *    | 0 <= amount
	 * @pre The given amount is not greater than 10000.
	 *    | amount <= 10000
	 */
	public void takeDamage(int amount) {
		this.health -= amount;
	}
}

class PlayerCharacter extends GameCharacter {
	
	/**
	 * Initializes this game character with the given health.
	 * @param health
	 * @pre The given health is nonnegative.
	 *     | 0 <= health
	 */
	public PlayerCharacter(int health) {
		super(health);
	}

}

class Main {
	
	/** @pre | y != 0 */
	public static int divide(int x, int y) {
		return x / y;
	}

	public static void main(String[] args) {
		// Success case
		GameCharacter c = new GameCharacter(5);
		c.takeDamage(3);
		divide(10, 3);

		try {
			// Failure case
			c.takeDamage(-3);
			System.err.println("No exception was thrown! :-(");
		} catch (AssertionError e) {
			e.printStackTrace();
		}

		try {
			// Failure case
			c.takeDamage(200_000);
			System.err.println("No exception was thrown! :-(");
		} catch (AssertionError e) {
			e.printStackTrace();
		}

		try {
			// Failure case
			new GameCharacter(-3);
			System.err.println("No exception was thrown! :-(");
		} catch (AssertionError e) {
			e.printStackTrace();
		}

		try {
			// Failure case
			new PlayerCharacter(-5);
			System.err.println("No exception was thrown! :-(");
		} catch (AssertionError e) {
			e.printStackTrace();
		}

		try {
			// Failure case
			divide(10, 0);
			System.err.println("No exception was thrown! :-(");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
	}

}


// Check that @pre or @post formal parts in the wrong place do not crash the compiler

/** @pre | false */
class MisplacedPrePost {
	/** @pre | false */
	int x;
}

/** @pre | false */
interface MisplacedPrePostInterface {
	
}

/** @pre | false */
@interface MisplacedPrePostAnnotation {
	
}

/** @pre | false */
enum MisplacedPrePostEnum {
	/** @pre | false */
	MISPLACED_PRE_POST_ENUM_CONSTANT;
}
