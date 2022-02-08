/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniela
 */
public class Knapsack {
     private int n;
    private double w[], v[]; //peso e valor dos itens
    private RatioVW vw[];  //vetor de itens com taxas valor/peso em ordem descrescente
    private boolean conflito[][];  //matriz de conflito 
    private double bestSolutionV;  //valor da melhor solu��o (maximiza��o)
    private double bestSolutionW;  //peso da melhor solu��o (maximiza��o)
    private String bestSolutionItens;    //itens na mochila da melhor solu��o
    private int solutionTotalItens; //total de itens da solu��o
    private double c;  //Capacidade m�xima da mochila
    //Flag para controlar continua��o da quando a inst�ncia atual � interrompida por tempo limite
    private boolean continua; 
    //Controle do tempo de execu��o:
    private long start, end;
    //Arquivo de sa�da:
    private BufferedWriter out;
    
    static class TuplaConflito{
         int de, para;  //o item 'de' n�o pode existir com o item 'para' na mochila 
         public TuplaConflito(int de, int para){
             this.de = de;
             this.para = para;
         }
    }
    
    private static class RatioVW implements Comparable<Object>{
        int index;  //o �ndice do item {0, 1, ..., n-1}
        double v, w, ratio;

        public RatioVW(int index, double v, double w){
            this.index = index;
            this.v =v;
            this.w = w;
            this.ratio = this.v/this.w;
        }
        @Override
        public int compareTo(Object o) {
            RatioVW aux = (RatioVW) o;
             if (this.ratio > aux.ratio)
                 return 1;
             else if (this.ratio < aux.ratio)
                 return -1;
             else 
                 return 0;  //iguais
        }
    }
    
    /*class InterruptTask extends TimerTask {
        public void run() {
            System.out.println("\nTime's up!");
            System.out.printf("Sooooolu��o w(%.0f) v(%.0f) itens(%s)", wMelhorSolucao, vMelhorSolucao, itensSolucao); 
            this.cancel();
            System.exit(0);
        }
    }*/
    
    public Knapsack(int n, double w[], double v[],  ArrayList<TuplaConflito> tuplas,
            double c, BufferedWriter out){
        this.n = n; 
        this.w = w; this.v = v; this.c = c;
        this.bestSolutionV = 0; // valor inicial melhor solu��o 
        this.bestSolutionW = 0; // peso inicial melhor solu��o
        this.bestSolutionItens = "";  // itens inclusos na melhor solu��o
        this.solutionTotalItens = 0; //total de itens na solu��o
        //para medida do tempo de execu��o entre start e and;
        this.start = 0; 
        this.end = Long.MAX_VALUE;
        //arq sa�da:
        this.out = out;
        
        String dados= String.format("%3s %3s %3s %9s\n", "-i-", "-w-", "-v-", "-ratio-");
        
        //Cria e preenche vw:
        this.vw = new RatioVW[n];
        for (int i=0; i < n; i++){
              this.vw[i] = new RatioVW(i, this.v[i], this.w[i]);
              dados+= String.format("%3d %3.0f %3.0f %3d(%4.1f)\n", i, w[i], v[i],this.vw[i].index, this.vw[i].ratio);
        }
        
        //Exibi��o de dados do problema:
        System.out.println("---- dados do problema:");
        System.out.println("n="+this.n + " capacidade: "+this.c);
        System.out.println(dados);
        
        //Ordena vw em ordem ascendente
        Arrays.sort(this.vw);  
        RatioVW aux[] = new RatioVW[this.n];
        //Reverte vw para ordem descendente:
        for(int i=0, j=n-1; i < n; i++, j--){
            aux[i] = this.vw[j];
        }
        this.vw = aux;
        
        //Instancia matriz de conflitos:
        this.conflito = new boolean[n][n];
        for(int i=0; i < n; i++)
            for(int j=0; j < n; j++)
                this.conflito[i][j] = false;        
        //Constroi matriz conflitos segundo a nova posi��o dos itens 'de' e 'para' de cada TuplaConflito:
        for(TuplaConflito t: tuplas){
           int novoDe=Integer.MIN_VALUE, novoPara=Integer.MAX_VALUE; 
           //busca �ndices novos para t.de e t.para em vw:
           for(int i= 0; i < n; i++){
               if (this.vw[i].index == t.de)
                   novoDe = i;
               if (this.vw[i].index == t.para)
                   novoPara = i;
           }
           if (novoDe == Integer.MIN_VALUE || novoPara == Integer.MAX_VALUE){
               System.out.println("Erro! Tupla conflito- De: "+t.de  + " Para: "+t.para);
               System.exit(0);
           }
           conflito[novoDe][novoPara] = true;
           conflito[novoPara][novoDe] = true;
        }

        dados= String.format("%3s %3s %3s %12s\n", "-i-", "-w-", "-v-", "-ratio-");
        
        //Preenche vetores w e v (na ordem da ration=v/w):
        for(int i=0; i < n; i++){
            this.w[i] = this.vw[i].w;   //vetor  de pesos em ordem ascendente
            this.v[i] = this.vw[i].v;   //vetor  de valores em ordem ascendente
            dados+= String.format("%3d %3.0f %3.0f %3d(%4.3f)\n", i, w[i], v[i],this.vw[i].index, this.vw[i].ratio); 
        }
        
        System.out.println("---- Dados do problema ordenados por ratio(descrescente)");
        System.out.println(dados);
        
        //Imprime matriz de conflitos:
        /*for(int i=0; i < n; i++){
            System.out.println("Matriz conflitos: ");
            for(int j=0; j < n; j++)
                if (this.conflito[i][j]){
                    System.out.print(" 1 ");
                }else{
                    System.out.print(" 0 ");
                }
            System.out.println(""); 
        }*/
        
    }
    
    //Inicia n� raiz da �rvore de busca para knapsack com conflitos usando Branch-and-Bound
    public void knapsackSolver(){
        int[] subSet= new int[n];
        for(int i=0; i < n; i++)
              subSet[i] = 0;
        
        //Inicia busca em n�s filhos da raiz:
        knapsackSolver(1, true, "1", this.w[0], this.v[0]); //inicia busca com item 1 
        knapsackSolver(1, false, "0", 0, 0); //inicia busca sem item 1        
    }
    
    //Resolve com recurs�o o knapsack via branch-and-bound
    //'i' � o n�vel atual da �rvore de espa�os de busca
    //'incluiItem' � true para incluir i-�simo item � solu��o parcial, false caso contr�rio
    //'subSet' bitString para indicar o subconjunto de itens inclusos:  1 na posi��o i indica inclus�o de i, ou 0 caso contr�rio.
    // 'w' peso da solu��o parcial 
    // 'v' valor da solu��o parcial 
    //private void knapsackSolver(int i, boolean incluiItem, int[] subSet, double w, double v){
    private void knapsackSolver(int i, boolean incluiItem, String subSet, double w, double v){
        //Indice de consulta nos vetores w, v, e wv
        int index = i-1;
        
        //Verifica restri��es se o subconjunto (solu��o parcial) possui mais de um item
        if(i > 1 && incluiItem && !atendeRestricoes(subSet)){    
             return;  //Termina busca se n�o atender restri��es
        }
        
        //Termina busca por capacidade excedida
        if (w > this.c)
             return;
      
        //Se n� folha atualiza se encontrada melhor solu��o
        if (i >= this.n){
            if (v > this.bestSolutionV){
                this.bestSolutionV = v;
                this.bestSolutionW = w;
                this.bestSolutionItens = this.getItensSolucao(subSet);
            }    
            return;        
        }
        
        //Calcula upperBound do n�
        double ub = v + (this.c - w)*this.vw[index+1].ratio;  //� o �ndice do pr�ximo item de maior taxa valor/peso do vetor vw
        
         //Verifica poda (branch)
         if (ub < this.bestSolutionV)
             return;  
       
        //Aprofunda busca em n�s filhos:
        knapsackSolver((i+1), true, subSet.toString()+"1", (w+this.w[index+1]), (v+this.v[index+1]));  //busca com inclus�o do pr�ximo item   
        knapsackSolver((i+1), false, subSet.toString()+"0", w, v); //busca sem incluir o pr�ximo item
     }
    
    //Recebe o bitString da solu��o parcial (subSet) e 
    //baseado no �tlimo item em subSet, verifica se h� 
    //restri��o desse com todos os itens anteriores:
    private boolean atendeRestricoes(String subSet){
        int indexDe = subSet.length()-1;  //indice do �ltimo item do subconjunto
        for(int i=0; i < indexDe;i++){
            if(subSet.charAt(i) == '1'){
                 if(this.conflito[indexDe][i]){
                      return false;
                 }
            }
        }
        return true;
    }
    
    //Recebe um bitString do subconjunto e retorna os itens relacionados numa String:
    private String getItensSolucao(String subset){
        System.out.println("Itens da solu��o em bitString: "+subset);
        ArrayList<Integer> itens= new ArrayList<>(); 
        this.solutionTotalItens = 0; 
        for(int i = 0; i < subset.length(); i++){
            if(subset.charAt(i) == '1'){
                itens.add(this.vw[i].index);
                this.solutionTotalItens++;
            }
        }    
        Integer[] itensMochila = itens.toArray(new Integer[itens.size()]);
        Arrays.sort(itensMochila);
        String itensSubset = "";
        for(int i = 0; i < this.solutionTotalItens; i++)  
               itensSubset+=itensMochila[i]+" ";
        System.out.println("Itens da solu��o: "+itensSubset);
        return itensSubset.trim();
    }
        
    public double getBestSolutionV() {
        return bestSolutionV;
    }

    public double getBestSolutionW() {
        return bestSolutionW;
    }

    public int getSolutionTotalItens(){
        return this.solutionTotalItens;
    }
    
    public String getBestSolutionItens() {
        return bestSolutionItens;
    }   
    
    public static void main(String[] args) {
        //System.out.println(args[0] + " "+args[1]);
  
        //Var locais:
        int n=0, m=0, de, para;
        double c=0, w[], v[];
        w=null; v=null;
        ArrayList<TuplaConflito> tuplas = new ArrayList<>();
        int inst, instLimite; 

        //int n = 5;  //numero de itens
        //double w[] = {7, 3, 4, 5, 1};
        //double v[] = {42, 12, 40, 25, 20};
        //tuplas.add(new TuplaConflito(2, 4));
         
        //Leitura dos dados do arquivo de inst�ncia
        try{
             //delimita repeti��es para a quantidade de arquivos informados
             if (args.length == 0){  //usa arquivos em 'resources'
                    inst=50; instLimite = 1000;
             }else{
                 inst=0; instLimite = args.length-2;              
             }
            do{
                //Var locais:
                 n=0; m=0; 
                 c=0; w=null; v=null;
                 tuplas = new ArrayList<>();
                //System.out.println(Knapsack.class.getResource(args[0]));
                File instancia;

                if (args.length == 0){
                    System.out.println(Knapsack.class.getResource("n"+inst+".KNPC"));
                    System.out.println(Knapsack.class.getResource("n"+inst+".KNPC").getFile());
                    instancia = new File(Knapsack.class.getResource("n"+inst+".KNPC").getFile());
                    inst+=50;
                }else{
                    instancia = new File(args[inst]); 
                    inst++;
                }   
                
                //Leitura dos dados do arquivo de inst�ncia:
                Scanner in= new Scanner(instancia);
                c = in.nextDouble();
                n = in.nextInt(); 
                //Cria vetores para peso(w) e lucro(v) e os preenche
                w = new double[n];
                v = new double[n];
                for (int i=0; i < n;i++){
                    v[i] = in.nextDouble();
                }
                for (int i=0; i < n;i++){
                    w[i] = in.nextDouble();
                }
                //L� n�mero de pares conflitantes
                m = in.nextInt();
                //L� pares conflitantes e gera lista
                for(int i=0; i < m; i++){
                    de = in.nextInt();
                    para = in.nextInt();
                    tuplas.add(new TuplaConflito(de, para));
                }
                System.out.println("Tamanho tuplas: "+tuplas.size());
                
                //Criar arquivo de sa�da
                File solucao;
                if (args.length == 0)              
                    solucao = new File("solucao.txt");
                else
                    solucao = new File(args[args.length-1]);

                if (!solucao.exists()){
                       solucao.createNewFile();
                }
                FileWriter fw = new FileWriter(solucao, true);
                BufferedWriter out = new BufferedWriter(fw); 

                //Cria��o instancia Knapsack
                final Knapsack k= new Knapsack(n, w, v, tuplas, c, out);
                k.continua =false; 
                //Timer timer = new Timer();
                //timer.schedule(k.new RemindTask(), 689*1000);
                //timer.schedule(k.new InterruptTask(), 10);

                                
                ActionListener taskPerformer = new ActionListener() {
                    public void actionPerformed(ActionEvent evt){
                       try{
                            System.out.println("\nTime's up!");
                            System.out.printf("Best solution now: w(%.0f) v(%.0f) itens(%s)", k.getBestSolutionW(), k.getBestSolutionV(), k.getBestSolutionItens()); 
                            //System.exit(0);
                            //Calcula o tempo de execu��o em milissegundos
                             double elapsedTime = (k.end - k.start)/1000.0;  

                             //Impress�o da solu��o no arquivo de sa�da:
                             String linha = String.format("n= %3d; Lucro total: %6.2f; "
                                     + "Itens: %40s; Total de itens: %2d; Tempo excedeu %8.5f ms", 
                                     k.n, k.getBestSolutionV(), k.getBestSolutionItens(),
                                     k.getSolutionTotalItens(), (1000*689));
                             out.append(linha);
                             out.newLine();
                             out.flush();
                             k.continua = true;
                             Thread.interrupted();
                       }catch(Exception ex){
                              Logger.getLogger(Knapsack.class.getName()).log(Level.SEVERE, null, ex);
                       }
                     }
                };
               
                //Cria o Timer para encerrar por limite de tempo:
                int delay = 1000*689; //milliseconds
                javax.swing.Timer upTime = new javax.swing.Timer(delay, taskPerformer);
                upTime.setRepeats(false);
                upTime.start();
                 
                k.start = System.nanoTime();  //inicia contagem em nanosegundos
                k.knapsackSolver(); //apenas um n� raiz 
                //Continua a execucao com outras inst�ncias se interrompida a inst�ncia por tempo limite:
                if (k.continua)
                    continue;
                if (upTime.isRunning())
                    upTime.stop();
                
                k.end = System.nanoTime();  //finaliza contagem em nanosegundos
                
                //Calcula o tempo de execu��o em milissegundos
                double elapsedTime = (k.end - k.start)/(1000.*1000.0);  
                
                System.out.printf("Final Best solution: w(%.0f) v(%.0f) itens(%s)",
                        k.getBestSolutionW(), k.getBestSolutionV(), k.getBestSolutionItens());
                
                //Impress�o da solu��o no arquivo de sa�da:
                String linha = String.format("n= %3d; Lucro total: %6.2f; "
                        + "Itens: %40s; Total de itens: %2d; Tempo: %8.5f ms", 
                        k.n, k.getBestSolutionV(), k.getBestSolutionItens(),
                        k.getSolutionTotalItens(), elapsedTime);
                out.append(linha);
                out.newLine();
                out.flush();
                
                //Fecha arquivos
                in.close();  
                out.close();
            }while(inst <=instLimite);                   
        } catch (Exception ex) {
            Logger.getLogger(Knapsack.class.getName()).log(Level.SEVERE, null, ex);
        }
     
   }
}
