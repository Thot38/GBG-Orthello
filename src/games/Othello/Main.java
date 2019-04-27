package games.Othello;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int z=0;
		for(int i =0; i < 64; i ++)
			if(ConfigOthello.heur[i] > 0)
				z+= ConfigOthello.heur[i];
		
		System.out.println(z);
	}

}
