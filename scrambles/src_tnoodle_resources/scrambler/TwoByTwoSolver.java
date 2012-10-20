package scrambler;

import java.util.Random;

public class TwoByTwoSolver {
	public TwoByTwoSolver() {
		initMoves();
		initPrun();
	}

	/***
	There are 8 "corner" cubies, numbered 0 to 7.
	The home positions of these cubies are labeled in the diagram below.
	Each corner cubie has three exposed faces, so there are three faces labelled with each number.
	Asterisks mark the primary facelet position.
	Orientation will be the number of clockwise rotations the primary facelet is from the primary facelet position where it is located.
	We take as a convention that the BLD corner (7) is always solved, because as there are no centers, we can orient the puzzle the way we want.
	Because of that, we only need to store information about the seven first corners (from 0 to 6).
	Also, we can allow only U, R and F moves without loosing any solution.

               +----------+
               |*1*    *2*|
               |    U     |
               |*0*    *3*|
    +----------+----------+----------+----------+
    | 1      0 | 0      3 | 3      2 | 2      1 |
    |     L    |    F     |    R     |    B     |
    | 7      4 | 4      5 | 5      6 | 6      7 |
    +----------+----------+----------+----------+
               |*4*    *5*|
               |    D     |
               |*7*    *6*|
               +----------+

	 ***/

	static final int N_PERM = 5040; // Number of permutations
	static final int N_ORIENT = 729; // Number of orientations
	static final int N_MOVES = 9; // Number of moves
	static final int MAX_LENGTH = 20; // Max length of solutions
	static final String[] moveToString = {"U", "U2", "U'", "R", "R2", "R'", "F", "F2", "F'"};
	static final String[] inverseMoveToString = {"U'", "U2", "U", "R'", "R2", "R", "F'", "F2", "F"};

	static final int[] fact = {1, 1, 2, 6, 24, 120, 720}; // fact[x] = x!
	
	/**
	 * To avoid that 2x2 scrambles always end with R' U', we should randomly select 
	 * the order of the moves to try. And the simplest way is to add a random offset.
	 */
	Random r_offset = new Random();

	/**
	 * Converts the list of cubies into a number representing the permutation of the cubies.
	 * @param cubies   cubies representation (ori << 3 + perm)
	 * @return         an integer between 0 and 5039 representing the permutation of 7 elements
	 */
	private int packPerm(int[] cubies){
		int idx = 0;
		int val = 0x6543210;
		for (int i=0; i<6; i++) {
			int v = ( cubies[i] & 0x7 ) << 2;
			idx = (7 - i) * idx + ((val >> v) & 0x7);
			val -= 0x1111110 << v;
		}
		return idx;
	}

	/**
	 * Converts an integer representing a permutation of 7 elements into a list of cubies.
	 * @param perm     an integer between 0 and 5039 representing the permutation of 7 elements
	 * @param cubies   cubies representation (ori << 3 + perm)
	 */
	private void unpackPerm(int perm, int[] cubies){
		int val = 0x6543210;
		for (int i=0; i<6; i++) {
			int p = fact[6-i];
			int v = perm / p;
			perm -= v*p;
			v <<= 2;
			cubies[i] = (val >> v) & 0x7;
			int m = (1 << v) - 1;
			val = (val & m) + ((val >> 4) & ~m);
		}
		cubies[6] = val;
	}

	/**
	 * Converts the list of cubies into a number representing the orientation of the cubies.
	 * @param cubies   cubies representation (ori << 3 + perm)
	 * @return         an integer between 0 and 728 representing the orientation of 6 elements (the 7th is fixed)
	 */
	private int packOrient(int[] cubies){
		int ori = 0;
		for (int i=0; i<6; i++){
			ori = 3 * ori + ( cubies[i] >> 3 );
		}
		return ori;
	}

	/**
	 * Converts an integer representing the orientation of 6 elements into a list of cubies.
	 * @param ori      an integer between 0 and 728 representing the orientation of 6 elements (the 7th is fixed)
	 * @param cubies   cubies representation (ori << 3 + perm)
	 */
	private void unpackOrient(int ori, int[] cubies){
		int sum_ori = 0;
		for (int i=5; i>=0; i--){
			cubies[i] = ( ori % 3 ) << 3;
			sum_ori += ori % 3;
			ori /= 3;
		}
		cubies[6] = (( 42424242 - sum_ori ) % 3 ) << 3; // Any number multiple of 3 greater than 15 works :)
	}

	/**
	 * Cycle four elements of an array.
	 * @param cubies   cubies representation (ori << 3 + perm)
	 * @param a        first element to cycle
	 * @param b        second element to cycle
	 * @param c        third element to cycle
	 * @param d        fourth element to cycle
	 * @param times    number of times to cycle
	 */
	private void cycle(int[] cubies, int a, int b, int c, int d, int times){
		int temp = cubies[d];
		cubies[d] = cubies[c];
		cubies[c] = cubies[b];
		cubies[b] = cubies[a];
		cubies[a] = temp;
		if( times > 1 )
			cycle(cubies, a, b, c, d, times - 1);
	}

	/**
	 * Cycle four elements of an array. Also orient clockwise first and third elements and counter-clockwise the other ones.
	 * @param cubies   cubies representation (ori << 3 + perm)
	 * @param a        first element to cycle
	 * @param b        second element to cycle
	 * @param c        third element to cycle
	 * @param d        fourth element to cycle
	 * @param times    number of times to cycle
	 */
	private void cycleAndOrient(int[] cubies, int a, int b, int c, int d, int times){
		int temp = cubies[d];
		cubies[d] = (cubies[c] + 8) % 24;
		cubies[c] = (cubies[b] + 16) % 24;
		cubies[b] = (cubies[a] + 8) % 24;
		cubies[a] = (temp + 16) % 24;
		if( times > 1 )
			cycleAndOrient(cubies, a, b, c, d, times - 1);
	}

	/**
	 * Apply a move on the cubies representation.
	 * @param cubies   cubies representation (ori << 3 + perm)
	 * @param move     move to apply to the cubies
	 */
	private void moveCubies(int[] cubies, int move){
		int face = move / 3;
		int times = ( move % 3 ) + 1;
		switch (face){
			case 0: // U face
				cycle(cubies, 0, 1, 2, 3, times);
				break;
			case 1: // R face
				cycleAndOrient(cubies, 3, 2, 6, 5, times);
				break;
			case 2: // F face
				cycleAndOrient(cubies, 0, 3, 5, 4, times);
				break;
		}
	}


	/**
	 * Fill the arrays to move permutation and orientation coordinates.
	 */
	private static int[][] movePerm = new int[N_PERM][N_MOVES];
	private static int[][] moveOrient = new int[N_ORIENT][N_MOVES];
	private void initMoves(){
		int[] cubies1 = new int[7];
		int[] cubies2 = new int[7];
		for (int perm=0; perm<N_PERM; perm++){
			unpackPerm(perm, cubies1);
			for (int move=0; move<N_MOVES; move++){
				System.arraycopy(cubies1, 0, cubies2, 0, 7);
				moveCubies(cubies2, move);
				int newPerm = packPerm(cubies2);
				movePerm[perm][move] = newPerm;
			}
		}

		for (int orient=0; orient<N_ORIENT; orient++){
			unpackOrient(orient, cubies1);
			for (int move=0; move<N_MOVES; move++){
				System.arraycopy(cubies1, 0, cubies2, 0, 7);
				moveCubies(cubies2, move);
				int newOrient = packOrient(cubies2);
				moveOrient[orient][move] = newOrient;
			}
		}
	}

	/**
	 * Fill the pruning tables for the permutation and orientation coordinates.
	 */
	private static int[] prunPerm = new int[N_PERM];
	private static int[] prunOrient = new int[N_ORIENT];
	private void initPrun(){

		for (int perm=0; perm<N_PERM; perm++)
			prunPerm[perm] = -1;
		prunPerm[0] = 0;

		int done = 1;
		for (int length=0; done<N_PERM; length++){
			for (int perm=0; perm<N_PERM; perm++){
				if( prunPerm[perm] == length ){
					for (int move=0; move<N_MOVES; move++){
						int newPerm = movePerm[perm][move];
						if( prunPerm[newPerm] == -1 ){
							prunPerm[newPerm] = length + 1;
							done++;
						}
					}
				}
			}
		}

		for (int orient=0; orient<N_ORIENT; orient++)
			prunOrient[orient] = -1;
		prunOrient[0] = 0;

		done = 1;
		for (int length=0; done<N_ORIENT; length++){
			for (int orient=0; orient<N_ORIENT; orient++){
				if( prunOrient[orient] == length ){
					for (int move=0; move<N_MOVES; move++){
						int newOrient = moveOrient[orient][move];
						if( prunOrient[newOrient] == -1 ){
							prunOrient[newOrient] = length + 1;
							done++;
						}
					}
				}
			}
		}
	}

	/**
	 * Search a solution from a position given by permutation and orientation coordinates
	 * @param perm       permutation coordinate to solve
	 * @param orient     orientation coordinate to solve
	 * @param depth      current depth of the search (first called with 0)
	 * @param length     the remaining number of moves we can apply
	 * @param last_move  what was the last move done (first called with an int >= 9)
	 * @param solution   the array containing the current moves done.
	 * @return           true if a solution was found (stored in the solution array)
	 *                   false if no solution was found
	 */
	private boolean search(int perm, int orient, int depth, int length, int last_move, int solution[]){

		/* If there are no moves left to try (length=0), returns if the current position is solved */
		if( length == 0 ) {
			return ( perm == 0 ) && ( orient == 0 );
		}

		/* Check if we might be able to solve the permutation or the orientation of the position
		 * given the remaining number of moves ('length' parameter), using the pruning tables.
		 * If not, there is no point keeping searching for a solution, just stop.
		 */
		if(( prunPerm[perm] > length ) || ( prunOrient[orient] > length ))
			return false;

		int RANDOM_OFFSET = r_offset.nextInt(N_MOVES);

		/* The recursive part of the search function.
		 * Try every move from the current position, and call the search function with the new position
		 * and the updated parameters (depth -> depth+1; length -> length-1; last_move -> move)
		 * We don't need to try a move of the same face as the last move.
		 */
		for( int _move=0; _move<N_MOVES; _move++){

			int move = (_move + RANDOM_OFFSET) % N_MOVES;

			// Check if the tested move is of the same face as the previous move (last_move).
			if(( move / 3 ) == ( last_move / 3 ))
				continue;
			// Apply the move
			int newPerm = movePerm[perm][move];
			int newOrient = moveOrient[orient][move];
			// Call the recursive function
			if( search( newPerm, newOrient, depth+1, length-1, move, solution )){
				// Store the move
				solution[depth] = move;
				return true;
			}
		}
		return false;
	}

	/**
	 * Generate a random position and solve it. Returns either the solution or the scramble (inverse solution)
	 * @param r         random int generator
	 * @param length    length of the desired solution
	 * @param inverse   do we want to return the solution or the scramble
	 * @return          a string representing the solution or the scramble of a random position
	 */
	public String randomScramble(Random r, int length, boolean inverse){
		int randomPerm = r.nextInt(N_PERM);
		int randomOrient = r.nextInt(N_ORIENT);
		int[] solution = new int[MAX_LENGTH];
		if( ! search(randomPerm, randomOrient, 0, length, 42, solution )) // No solution was found
				return "";

		StringBuffer scramble = new StringBuffer(MAX_LENGTH*3);
		if( inverse ){
			scramble.append(inverseMoveToString[solution[length-1]]);
			for (int l=length-2; l>=0; l--){
				scramble.append(" " + inverseMoveToString[solution[l]]);
			}
		} else {
			scramble.append(moveToString[solution[0]]);
			for (int l=1; l<length; l++){
				scramble.append(" " + moveToString[solution[l]]);
			}
		}

		return scramble.toString();
	}
	
}
