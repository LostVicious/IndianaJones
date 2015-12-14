package indianaJones;

import java.util.ArrayList;

public class Performance {
	int nTrade = 0;
	float percentualeDiSuccesso = 0;
	int guadagnoMedio = 0;
	int guadagnoTotale = 0;
	float profitLoss = 0;
	float sharpRatio = 0;
	ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
	
	public Performance(int nTrade, float percentualeDiSuccesso,
			int guadagnoMedio, int guadagnoTotale, float sharpRatio,
			ArrayList<TradeResult> trades) {
		super();
		this.nTrade = nTrade;
		this.percentualeDiSuccesso = percentualeDiSuccesso;
		this.guadagnoMedio = guadagnoMedio;
		this.guadagnoTotale = guadagnoTotale;
		this.sharpRatio = sharpRatio;
		this.trades = trades;
	}


	public Performance(ArrayList<TradeResult> trades) {
		this.trades = trades;
		this.nTrade = trades.size();
		if (nTrade>0) {
			int nPositivi = 0;
			float guadagnoTotale = 0;
			float sommaProfitti = 0, sommaPerdite = 0;
			for (TradeResult t : trades) {
				guadagnoTotale += t.guadagno;
				if (t.guadagno>0) {
					sommaProfitti += t.guadagno;
					nPositivi++;
				} else {
					sommaPerdite += -t.guadagno;
				}
			}
			this.percentualeDiSuccesso = (float)nPositivi/nTrade;
			this.guadagnoMedio = (int)guadagnoTotale/nTrade;
			this.guadagnoTotale = (int)guadagnoTotale;
			this.profitLoss = sommaProfitti/sommaPerdite;
		}
		
		//calcoliamo lo sharp
		this.sharpRatio = 0;
		if (nTrade>1) {
			//calcoliamo sommatoria scarti quadratici
			float sommaScarti = 0;
			for (TradeResult t : trades) {
				sommaScarti+= Math.pow(t.guadagno - guadagnoMedio,2);
			}
			float stdev = (float)Math.sqrt(sommaScarti/(nTrade-1));
			this.sharpRatio = guadagnoMedio/stdev;
		}
	}


	@Override
	public String toString() {
		return "Performance [nTrade=" + nTrade + ", percentualeDiSuccesso="
				+ percentualeDiSuccesso + ", guadagnoMedio=" + guadagnoMedio
				+ ", guadagnoTotale=" + guadagnoTotale + ", profitLoss="
				+ profitLoss + ", sharpRatio=" + sharpRatio + "]";
	}
	
	
}
