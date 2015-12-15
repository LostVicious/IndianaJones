package indianaJones;

import indianaJones.KDTree.Euclidean;
import indianaJones.KDTree.SearchResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Evaluation {
	Connection conn;
	DateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public Evaluation() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
	        conn = DriverManager.getConnection("jdbc:mysql://localhost/?user=root&password=zxcvbnm");
	        //seleziono il database
	        conn.setCatalog("hedgefund");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String dataQuery(String codalfa, String timeCondition) {
		String query = "SELECT c.tempo, i.valore AS  vwapRatioFTSE, `minuto`, `vwapRatio`, `deltaVwap`, `ipercomprato60_85`, `gainLong99_99`, gainLong25_22, bookLiquidity, bookImbalance, volatility "+
						"FROM (SELECT * "+ 
						"FROM consolidata2minuti c "+
						"where codalfa='"+codalfa+"' "+
						"AND "+timeCondition+ 
						") AS c, indicatori i "+
						"WHERE i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) "+ 
						"ORDER by c.tempo ";
		return query;
	}
	
	void crossValidation(String codalfa, int N) {
		System.out.println("=== CROSS VALIDATION n="+N+" ===");
		Date min = new Date(0),max = new Date(0);
		long deltaT = Long.MAX_VALUE;
		ArrayList<Performance> performances = new ArrayList<Performance>();
		String query = "SELECT MIN(tempo) as mi, MAX(tempo) AS ma FROM consolidata2minuti WHERE codalfa='"+codalfa+"'";
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()) {
				min = rs.getDate("mi");
				max = rs.getDate("ma");
			}
			deltaT = (max.getTime()-min.getTime())/N;
			//qui abbiamo min max e deltaT
			
			
			for (long startT=min.getTime();startT<max.getTime();startT+=deltaT) {
				System.out.println("=== GRUPPO test: "+new Date(startT));
				
				//genera tree prendendo dati da mi a startT e da startT+deltaT a ma
				//carico dati Training:
				query=dataQuery(codalfa, "(c.tempo>='"+tsFormat.format(min)+"' AND c.tempo<'"+tsFormat.format(new Date(startT))+"') "+
										"OR (c.tempo>='"+tsFormat.format(new Date(startT+deltaT))+"' AND c.tempo<='"+tsFormat.format(max)+"')");
				System.out.println(query);
				//carico i dati nella Mappa
				Euclidean<PuntoConsolidato> tree = new Euclidean<PuntoConsolidato>(2); //2 dimensioni
				rs = stmt.executeQuery(query);
				ArrayList<Float> t = new ArrayList<Float>();
				while(rs.next()) {
					double vwapRatio = rs.getFloat("vwapRatio");
					double vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
					Date tempo = rs.getDate("tempo");
					//tree.addPoint(new double[]{vwapRatio,vwapRatioFTSE}, new PuntoConsolidato(vwapRatio,vwapRatioFTSE,rs.getFloat("gainLong99_99"),tempo));
					tree.addPoint(new double[]{vwapRatio,vwapRatioFTSE}, new PuntoConsolidato(vwapRatio,vwapRatioFTSE,rs.getFloat("gainLong25_22"),tempo));
				}
				
				//testa il tree con dati da startT a starT+deltaT:
				query=dataQuery(codalfa, "c.tempo>='"+tsFormat.format(new Date(startT))+"' AND c.tempo<'"+tsFormat.format(new Date(startT+deltaT))+"' "); 
				
				//blacklist dei giorni, trada sulla prima occasione della giornata
				ArrayList<Integer> giorniGiaTradati = new ArrayList<Integer>();
				int mil = 1000*60*60*24; //milliseconds in a day
				
				int nTrade=0,nTradePositivi=0,guadagnoTotale=0;
				ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
				
				rs = stmt.executeQuery(query);
				while(rs.next()) {
					double vwapRatio = rs.getFloat("vwapRatio");
					double vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
					Date tempo = rs.getDate("tempo");
					int PointDay = (int)tempo.getTime()/mil;
					if (!giorniGiaTradati.contains(PointDay) && condizioni(tree,vwapRatio,vwapRatioFTSE)) {
						giorniGiaTradati.add(PointDay);
						//float guadagno = rs.getFloat("gainLong99_99");
						float guadagno = rs.getFloat("gainLong25_22");
						trades.add(new TradeResult(guadagno));
						System.out.println("Entrato "+tempo+"\tGuadagno:\t"+guadagno);
						nTrade++;
						if (guadagno>0) nTradePositivi++;
						guadagnoTotale += guadagno;
					}
				}
				performances.add(new Performance(trades));
				//output separatore
				//salva guadagnoMedio in array
			
			//outputta i guadagni medi delle varie crossvalidation, e il guadagno medio totale
			
			//return guadagno medio totale
			}
			System.out.println("====== CROSS VALIDATION TERMINATA =======");
			for (Performance p : performances) {
				System.out.println(p);
			}
			System.out.println("====== PERFORMANCE COMPLESSIVA: =======");
			ArrayList<TradeResult> tradesTotali = new ArrayList<TradeResult>();
			for (Performance p : performances) {
				tradesTotali.addAll(p.trades);
			}
			System.out.println(new Performance(tradesTotali));
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	boolean condizioni(KDTree<PuntoConsolidato> tree,double vwapRatio,double vwapRatioFtse) {
		ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{vwapRatio,vwapRatioFtse}, 1000);
		int mil = 1000*60*60*24; //milliseconds in a day
		double sommaDist=0, sommaGuadagno=0;
		ArrayList<Integer> blacklistedDays = new ArrayList<Integer>();
		int nViciniUsati=0, nPositivi=0,nGiorniNelVicinato=0;
		float Tot=0;
		for (SearchResult<PuntoConsolidato> p : r) {
			int pointDay = (int)p.payload.tempo.getTime()/mil;
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
				sommaDist += p.distance*100000;
				sommaGuadagno += p.payload.guadagno*(1-(p.distance/maxD));
				if (p.payload.guadagno>0) nPositivi++;
				Tot+=1-(p.distance/maxD);
			}
			//if (nViciniUsati>=K) break;
		}

		if (nGiorniNelVicinato>=3 && (float)nPositivi/nViciniUsati >0.60) {
			return true;
		}
		return false;
	}
	
	boolean condizioniSharpe(KDTree<PuntoConsolidato> tree,double vwapRatio,double vwapRatioFtse) {
		//MAPPA SHARP
		ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{vwapRatio,vwapRatioFtse}, 1000);
		int mil = 1000*60*60*24; //milliseconds in a day
		ArrayList<Integer> blacklistedDays = new ArrayList<Integer>();		
		int nViciniUsati=0, nPositivi=0,nGiorniNelVicinato=0;
		float Tot=0;
		ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
		for (SearchResult<PuntoConsolidato> p : r) {
			int pointDay = (int)p.payload.tempo.getTime()/mil;
			//System.out.println("d="+p.distance);
			double maxD=0.00003;
			if (!blacklistedDays.contains(pointDay) && p.distance<maxD) {
			//if (p.distance<maxD) {
				//System.out.println(p.payload);
				if (!blacklistedDays.contains(pointDay)) {
					blacklistedDays.add(pointDay);
					nGiorniNelVicinato++;
					//trades.add(new TradeResult((float)p.payload.guadagno));
				}
				trades.add(new TradeResult((float)p.payload.guadagno));
				nViciniUsati++;
				Tot+=1-(p.distance/maxD);
			}
			//if (nViciniUsati>=K) break;
		}
		
		Performance performanceVicinato = new Performance(trades);
		
		if (nGiorniNelVicinato>=2 && performanceVicinato.sharpRatio>0.3)
			return true;
		return false;
	}

	void testaTitoli() {
		//per titolo in titoli 
		//output crossvalidation(titolo)
	}
}
