package indianaJones;

import indianaJones.KDTree.SearchResult;

import java.util.ArrayList;

public class metodoSharpe implements Metodo {
	
	public float result = 0.0f;
	public Object getLastResult() {
		return result;
	}
	
	public boolean call(KDTree<PuntoConsolidato> tree,double vwapRatio,double vwapRatioFtse) {
		//MAPPA SHARP
		ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{vwapRatio,vwapRatioFtse}, 1000);
		int mil = 1000*60*60*24; //milliseconds in a day
		ArrayList<Integer> blacklistedDays = new ArrayList<Integer>();		
		int nGiorniNelVicinato=0;
		ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
		for (SearchResult<PuntoConsolidato> p : r) {
			int pointDay = (int)(p.payload.tempo.getTime()/mil);
			//System.out.println("d="+p.distance);
			double maxD=0.00004;
			//if (!blacklistedDays.contains(pointDay) && p.distance<maxD) {
			if (p.distance<maxD) {
				//System.out.println(p.payload);
				if (!blacklistedDays.contains(pointDay)) {
					blacklistedDays.add(pointDay);
					nGiorniNelVicinato++;
					//trades.add(new TradeResult((float)p.payload.guadagno));
				}
				trades.add(new TradeResult((float)p.payload.guadagno));
			}
		}
		
		Performance performanceVicinato = new Performance(trades);
		this.result = performanceVicinato.sharpRatio;
		if (nGiorniNelVicinato>=2) {
			if (performanceVicinato.sharpRatio>0.3) {
				return true;
			}
		}
		return false;
	}
	
	
}
