package indianaJones;

public class Performance {
	int nTrade = 0;
	float percentualeDiSuccesso = 0;
	int guadagnoMedio = 0;
	int guadagnoTotale = 0;
	public Performance(int nTrade, float percentualeDiSuccesso,
			int guadagnoMedio, int guadagnoTotale) {
		super();
		this.nTrade = nTrade;
		this.percentualeDiSuccesso = percentualeDiSuccesso;
		this.guadagnoMedio = guadagnoMedio;
		this.guadagnoTotale = guadagnoTotale;
	}
	@Override
	public String toString() {
		return "Performance [nTrade=" + nTrade + ", percentualeDiSuccesso="
				+ percentualeDiSuccesso + ", guadagnoMedio=" + guadagnoMedio
				+ ", guadagnoTotale=" + guadagnoTotale + "]";
	}
	
	
}
