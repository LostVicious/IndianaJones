package indianaJones;

import indianaJones.KDTree.SearchResult;

import java.awt.print.Printable;
import java.util.ArrayList;

public class MetodoPercentuale implements Metodo {
	
	public float result = 0.0f;
	
	public Object getLastResult() {
		return result;
	}
	
	public boolean call(KDTree<PuntoConsolidato> tree,double vwapRatio,double vwapRatioFtse) {
		ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{vwapRatio,vwapRatioFtse}, 1000);
		int mil = 1000*60*60*24; //milliseconds in a day
		ArrayList<Integer> blacklistedDays = new ArrayList<Integer>();
		int nViciniUsati=0, nPositivi=0,nGiorniNelVicinato=0;
		for (SearchResult<PuntoConsolidato> p : r) {
			int pointDay = (int)(p.payload.tempo.getTime()/mil);
			//System.out.println("d="+p.distance);
			double maxD=0.00003;
			//if (!blacklistedDays.contains(pointDay) && p.distance<maxD) {
			if (p.distance<maxD) {
				//System.out.println(p.payload);
				if (!blacklistedDays.contains(pointDay)) {
					blacklistedDays.add(pointDay);
					nGiorniNelVicinato++;
				}
				nViciniUsati++;
				if (p.payload.guadagno>0) nPositivi++;
			}
			//if (nViciniUsati>=K) break;
		}
		
		this.result = 0;
		if (nGiorniNelVicinato>=3) {
			this.result = (float)nPositivi/nViciniUsati;
			if ((float)nPositivi/nViciniUsati >0.69) {
				return true;
			}
		}
		return false;
	}
	
}
