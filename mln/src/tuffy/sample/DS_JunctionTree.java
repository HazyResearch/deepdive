package tuffy.sample;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

import tuffy.infer.MRF;
import tuffy.infer.ds.GAtom;
import tuffy.infer.ds.GClause;
import tuffy.util.Enumerator;
import tuffy.util.ExceptionMan;

public class DS_JunctionTree {
	
	public MRF mrf = null;
	
	Graph<GAtom> graph = null;
	
	Graph<Clique<Node<GAtom>>> jt = null;
	
	public int getTreeWidth(){
		int width = Integer.MIN_VALUE;
		for(Node<Clique<Node<GAtom>>> node : jt.nodes){
			if(node.content.nodes.size() >= width){
				width = node.content.nodes.size();
			}
		}
		return width;
	}
	
	public DS_JunctionTree (MRF mrf){
		this.mrf = mrf;
		if(this.mrf.adj.isEmpty()){
			this.mrf.buildIndices();
		}
		
		this.mrf.compile();
		
		this.graph = new Graph<GAtom>();
		for(GAtom gatom : this.mrf.atoms.values()){
			this.graph.addNode(gatom);
		}
		
		for(GClause gc : this.mrf.clauses){
			for(Integer lit1 : gc.lits){
				int aid1 = Math.abs(lit1);
				Node<GAtom> node1 = this.graph.getNode(this.mrf.atoms.get(aid1));
				
				for(Integer lit2 : gc.lits){
					
					if(lit1 == lit2){
						continue;
					}
					
					int aid2 = Math.abs(lit2);
					Node<GAtom> node2 = this.graph.getNode(this.mrf.atoms.get(aid2));

					this.graph.addEdge(node1, node2);
				}
			}
		}
		
		Graph<GAtom> cordal = this.graph.triangulate(CORDAL_STRATEGY.MIN_NEIGHBOR);
		//cordal.print();
		
		jt = cordal.getCliques();
		
		//for(Node<Clique<Node<GAtom>>> node : jt.nodes){
		//	Clique<Node<GAtom>> clique = node.content;
		//	this.initCliquePotential(clique);
		//	//System.out.println(clique.logPotentials);
		//}
		
		
		
		//System.out.println(this.mrf.atoms.size() + " atoms component...");
		//jt.print();
		
		
	}
	
	
	public HashSet<GAtom> getNeighbors(GAtom atom){
		HashSet<GAtom> rs = new HashSet<GAtom>();
		for(GClause gc : this.mrf.adj.get(atom)){
			for(int lid : gc.lits){
				rs.add(this.mrf.atoms.get(Math.abs(lid)));
			}
		}
		return rs;
	}
	
	public class Node<E>{
		
		public E content;
		public HashSet<Node<E>> neighbors = new HashSet<Node<E>>();
		
		public int label = 0;
		
		public Node(E _content){
			content = _content;
		}
		
		public void addNeighbor(Node<E> neighbor){
			this.neighbors.add(neighbor);
		}
		
		public String toString(){
			return "N:" + content;
		}
		
		HashSet<Node<E>> children = new HashSet<Node<E>>();
		Node<E> parent = null;
		
	}
	
	public enum CORDAL_STRATEGY {MIN_NEIGHBOR, MIN_FILL};
	
	public void initCliquePotential(Clique<Node<GAtom>> clique){
		
		clique.atoms = new ArrayList<GAtom>();
		for(Node<GAtom> node : clique.nodes){
			clique.atoms.add(node.content);
		}
		
		
		Enumerator em = new Enumerator(clique.atoms.size());
		while(true){
			
			int[] config = em.next();
			
			if(config == null){
				break;
			}
			
			Double potential = 0.0;
			
			BitSet world = new BitSet();
			
			for(int i=0;i<config.length;i++){
				if(config[i] == 1){
					world.set(this.mrf.localAtomID.get(clique.atoms.get(i)));
				}
			}
			
			potential = -this.mrf.getCost(world);

			//System.out.println(potential);
			
			clique.logPotentials.put(world, potential);
			
		}
		
		
	}
	
	public class Clique<E> implements Cloneable{
		HashSet<E> nodes = new HashSet<E>();
		
		public ArrayList<GAtom> atoms = null;
		
		//TODO: change to a faster version
		public HashMap<BitSet, Double> logPotentials = new HashMap<BitSet, Double>();
		
		
		
		public void addElement(E ele){
			nodes.add(ele);
		}
		
		public String toString(){
			return this.nodes.size() + " nodes clique: " + this.nodes;
		}
		
	}
	
	public class Graph<E> implements Cloneable{
		
		public HashSet<Node<E>> nodes = new HashSet<Node<E>>();
		public HashMap<E, Node<E>> content2nodes = new HashMap<E, Node<E>>();
		
		public Graph<Clique<Node<E>>> getCliques(){
			
			Graph<Clique<Node<E>>> rs = new Graph<Clique<Node<E>>>();
			
			ArrayList<Clique<Node<E>>> cliques = new ArrayList<Clique<Node<E>>>();
			int prev_card = 0;
			int s = -1;
			HashSet<Node<E>> L = new HashSet<Node<E>>();
			HashSet<Node<E>> remains = new HashSet<Node<E>>();
			
			HashMap<Node<E>, Clique<Node<E>>> node2cliques = 
					new HashMap<Node<E>, Clique<Node<E>>>();
			
			remains.addAll(nodes);
			
			int i = nodes.size();
			for(i=i; i>=1; i--){
				
				int max = -1;
				Node<E> maxnode = null;
				for(Node<E> n : remains){
					int card = 0;
					for(Node<E> nj : n.neighbors){
						if(L.contains(nj)){
							card ++;
						}
					}
					if(max <= card){
						max = card;
						maxnode = n;
					}
				}
				
				int new_card = max;
				Node<E> vi = maxnode;
				maxnode.label = i;
				
				if(new_card <= prev_card){
					s = s + 1;
					Clique<Node<E>> cliq = new Clique<Node<E>>();
					for(Node<E> nj : vi.neighbors){
						if(L.contains(nj)){
							cliq.addElement(nj);
						}
					}
					cliques.add(cliq);
					rs.addNode(cliq);
					
					if(new_card != 0){
						
						int k = Integer.MAX_VALUE;
						Node<E> vk = null;
						for(Node<E> nj : cliq.nodes){
							if(nj.label <= k){
								k = nj.label;
								vk = nj;
							}
						}
						Clique<Node<E>> Kp = node2cliques.get(vk);
						rs.addEdge(rs.getNode(cliq), rs.getNode(Kp));
						
					}
					
					
				}
				
				node2cliques.put(vi, cliques.get(s));
				cliques.get(s).addElement(vi);
				L.add(vi);
				prev_card = new_card;
				
				remains.remove(vi);
				
			}
			
			return rs;
		
		}
		
		public void addNode(E content){
			if(content2nodes.containsKey(content)){
				return;
			}else{
				Node<E> n = new Node<E>(content);
				nodes.add(n);
				content2nodes.put(content, n);
			}
		}
		
		public Node<E> getNode(E content){
			return content2nodes.get(content);
		}
		
		public void addEdge(Node<E> node1, Node<E> node2){
			if(node1 == node2){
				return;
			}
			node1.addNeighbor(node2);
			node2.addNeighbor(node1);
		}
		
		public HashSet<Node<E>> getNeighbors(Node<E> node1){
			return node1.neighbors;
		}
		
		public Graph<E> getSameGraph(){
			
			Graph<E> rs = new Graph<E>();
			for(Node<E> n : this.nodes){
				rs.addNode(n.content);
			}
			for(Node<E> n1 : this.nodes){
				Node<E> rn1 = rs.getNode(n1.content);
				
				for(Node<E> n2 : n1.neighbors){
					Node<E> rn2 = rs.getNode(n2.content);
					rs.addEdge(rn1, rn2);
				}
			}
			
			return rs;
		}
		
		public void print(){
			
			System.out.println("### " + nodes.size() + " nodes :");
			for(Node node : nodes){
				System.out.println("###   " + node.content + " " + node.neighbors.size() + " edges...");
			}
			System.out.println("###");			
			
		}
		
		
		public Graph<E> triangulate(CORDAL_STRATEGY strategy){
			Graph<E> rs = this.getSameGraph();
				
			HashSet<Node<E>> remains = (HashSet<Node<E>>) rs.nodes.clone();
			while(! remains.isEmpty()){
			
				Double min = Double.MAX_VALUE;
				Node<E> tosel = null;
				
					//TODO: Slow
				for(Node<E> node : remains){
					double cost = -1;
					if(strategy == CORDAL_STRATEGY.MIN_NEIGHBOR){
						HashSet<Node<E>> neighbors = (HashSet<Node<E>>) node.neighbors.clone();
						neighbors.retainAll(remains);
						cost = neighbors.size();
					}else{
						ExceptionMan.die("not support yet!");
					}
					
					if(cost <= min){
						min = cost;
						tosel = node;
					}
				}
				
				for(Node<E> n1: tosel.neighbors){
					for(Node<E> n2 : tosel.neighbors){
						
						if(remains.contains(n1) && remains.contains(n2)){
						
							if(n1 != n2){
								rs.addEdge(n1, n2);
							}
							
						}
					}
				}
				
				remains.remove(tosel);
				
			}
			
			return rs;			
		}
		
		
		
		
	}
	
}

















