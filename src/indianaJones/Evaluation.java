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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Evaluation {
	Connection conn;
	DateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	int mil = 1000*60*60*24; //milliseconds in a day
	
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
		String query = "SELECT codalfa, c.tempo, i.valore AS  vwapRatioFTSE, `minuto`, `vwapRatio`, `deltaVwap`, `ipercomprato60_85`, `gainLong99_99`, gainLong25_22, bookLiquidity, bookImbalance, volatility "+
						"FROM (SELECT * "+ 
						"FROM consolidata2minuti c "+
						"where codalfa='"+codalfa+"' "+
						"AND ("+timeCondition+ 
						")) AS c, indicatori i "+
						"WHERE i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) "+ 
						"ORDER by c.tempo ";
		return query;
	}
	
	void crossValidation(String codalfa, int N) {
		this.crossValidation(codalfa, N,new Euclidean<PuntoConsolidato>(2));
	}
	
	Euclidean<PuntoConsolidato> generateTreeAll(String timeCondition) {
		String query = "SELECT codalfa, c.tempo, i.valore AS  vwapRatioFTSE, `minuto`, `vwapRatio`, `deltaVwap`, `ipercomprato60_85`, `gainLong99_99`, gainLong25_22, bookLiquidity, bookImbalance, volatility "+
				"from consolidata2minuti c, indicatori i "+  
				"where ("+timeCondition+") AND i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) " +
				"order by c.tempo";
		Euclidean<PuntoConsolidato> result = new Euclidean<PuntoConsolidato>(2);
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()) {
				PuntoConsolidato p = new PuntoConsolidato(rs);
				result.addPoint(new double[]{p.vwapRatio,p.vwapRatioFTSE}, p);
			}
		}  catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	ArrayList<PuntoConsolidato> puntiTitoloPeriodo(String codalfa,String timeCondition) {
		ArrayList<PuntoConsolidato> punti = new ArrayList<PuntoConsolidato>();
		String query = dataQuery(codalfa, timeCondition);
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()) {
				punti.add(new PuntoConsolidato(rs));
			}
		}  catch (Exception e) {
			e.printStackTrace();
		}
		return punti;
	}
	
	Performance testSampleGroup(Collection<PuntoConsolidato> points, Euclidean<PuntoConsolidato> tree, Metodo metodo) {
		ArrayList<Integer> giorniGiaTradati = new ArrayList<Integer>();
		ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
		for (Iterator<PuntoConsolidato> i=points.iterator(); i.hasNext(); ) {
			PuntoConsolidato p = i.next();
			int pointDay = (int)(p.tempo.getTime()/mil);
			if (!giorniGiaTradati.contains(pointDay) && metodo.call(tree,p.vwapRatio,p.vwapRatioFTSE)) {
				giorniGiaTradati.add(pointDay);
				trades.add(new TradeResult((float)p.guadagno));
				//System.out.println("Entrato "+p.tempo+"\tGuadagno:\t"+(p.guadagno>0 ? G.ANSI_GREEN:G.ANSI_RED) + p.guadagno + G.ANSI_RESET);
			}
		 }
		return new Performance(trades);
	}
	
	Performance crossValidation(String codalfa, int N, Euclidean<PuntoConsolidato> defaultTree) {
		boolean printall = false;
		System.out.println("=== CROSS VALIDATION "+codalfa+" n="+N+" ===");
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
			
			//prendo tutti i dati per questo ticker:
			query=dataQuery(codalfa, "1=1");
			if (printall) System.out.println(query);
			TreeMap<Date, PuntoConsolidato> dati = new TreeMap<Date, PuntoConsolidato>();
			rs = stmt.executeQuery(query);
			while(rs.next()) {
				Date tempo = rs.getTimestamp("tempo");
				Float vwapRatio = rs.getFloat("vwapRatio");
				Float vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
				Float guadagno = rs.getFloat("gainLong99_99");
				dati.put(rs.getTimestamp("tempo"), new PuntoConsolidato(rs.getString("codalfa"),vwapRatio, vwapRatioFTSE, guadagno, tempo));
			}
			
			for (long startT=min.getTime();startT<max.getTime();startT+=deltaT) {
				if (printall) System.out.println("=== GRUPPO test: "+new Date(startT));
				//carico i dati nella Mappa
				Euclidean<PuntoConsolidato> tree = new Euclidean<PuntoConsolidato>(2); //2 dimensioni
				
				if (defaultTree.size()>0) tree = defaultTree;
				else {
					TreeMap<Date, PuntoConsolidato> trainingSet = new TreeMap<Date, PuntoConsolidato>();
					trainingSet.putAll(dati.subMap(min, new Date(startT)));
					trainingSet.putAll(dati.subMap(new Date(startT+deltaT), max));
					
					if (printall) System.out.println("Dati di training: "+trainingSet.size());
					
					for (Map.Entry<Date, PuntoConsolidato> e : trainingSet.entrySet()) {
						PuntoConsolidato p = e.getValue();
						tree.addPoint(new double[]{p.vwapRatio,p.vwapRatioFTSE}, new PuntoConsolidato(p.codalfa,p.vwapRatio,p.vwapRatioFTSE,p.guadagno,p.tempo));
					}
				}
				TreeMap<Date, PuntoConsolidato> testSet = new TreeMap<Date, PuntoConsolidato>();
				testSet.putAll(dati.subMap(new Date(startT),new Date(startT+deltaT)));
				
				if (printall) System.out.println("Dati di testing: "+testSet.size());
				
				performances.add(testSampleGroup(testSet.values(), tree,new MetodoPercentuale()));
			
			//outputta i guadagni medi delle varie crossvalidation, e il guadagno medio totale
			
			//return guadagno medio totale
			}
			if (printall) System.out.println("====== CROSS VALIDATION TERMINATA =======");
			for (Performance p : performances) {
				if (printall) System.out.println(p);
			}
			if (printall) System.out.println("====== PERFORMANCE COMPLESSIVA: =======");
			ArrayList<TradeResult> tradesTotali = new ArrayList<TradeResult>();
			for (Performance p : performances) {
				tradesTotali.addAll(p.trades);
			}
			System.out.println(new Performance(tradesTotali));
			return new Performance(tradesTotali);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Performance(new ArrayList<TradeResult>());
	}
	
	static boolean condizioni(KDTree<PuntoConsolidato> tree,double vwapRatio,double vwapRatioFtse) {
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

		if (nGiorniNelVicinato>=3 && (float)nPositivi/nViciniUsati >0.71) {
			return true;
		}
		return false;
	}
	
	static boolean condizioniSharpe(KDTree<PuntoConsolidato> tree,double vwapRatio,double vwapRatioFtse) {
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
		
		if (nGiorniNelVicinato>=2 && performanceVicinato.sharpRatio>0.3)
			return true;
		return false;
	}

	void testaTitoli() {
		//per titolo in titoli 
		//output crossvalidation(titolo)
	}
}
