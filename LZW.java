import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Scanner;


public class LZW {

    public static final int BITS_POR_INDICE = 12;
    public static int versao = -1;
    private static Scanner console = new Scanner(System.in);


    public static void main(String[] args){
        
        try {
            int opcao;
            do {
                System.out.println("\n\n MENU DE BACKUP");
                System.out.println("------------");
                System.out.println("\n1) Realizar novo backup dos dados");
                System.out.println("2) Recuperar dados de uma versão específica de backup.");
                System.out.println("\n0) Voltar");
                System.out.println("\nOpção: ");

                try {
                    opcao = Integer.valueOf(console.nextLine());
                } catch(NumberFormatException e) {
                    opcao = -1;
                }

                switch(opcao) {
                    case 1:
                        setIndiceAtual();
                        versao = getIndiceAtual();
                        File folder = new File("backup_versao" + versao);
                        if (!folder.exists()) {
                            boolean result = folder.mkdir();
                            if (result) {
                                System.out.println("Diretório criado com sucesso: ");
                            } else {
                                System.out.println("Falha ao criar o diretório: ");
                            }
        
                        } else {
                            System.out.println("O diretório já existe: ");
                        }
                        createBackup();
                        break;
                    case 2:
                        int option;
                        System.out.println("\n\nQual versão de backup voce gostaria de recuperar (Por exemplo, digite 1 para backup_versao1): ");
                        try {
                            option = Integer.valueOf(console.nextLine());
                        } catch(NumberFormatException e) {
                            option = -1;
                        }
                        File folder_2 = new File("backup_versao" + option + "_decodificado");
                        if (!folder_2.exists()) {
                            boolean result = folder_2.mkdir();
                            if (result) {
                                System.out.println("Diretório criado com sucesso: ");
                            } else {
                                System.out.println("Falha ao criar o diretório: ");
                            }

                        } else {
                            System.out.println("O diretório já existe: ");
                        }
                        descompactarBackup(option);
                        break;
                    case 0:
                        break;
                    default:
                        System.out.println("Opção invalida.");
                }
            }while(opcao != 0);


        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void descompactarBackup(int option) {
        try {
            File folder = new File("backup_versao" + option);
            File[] listFiles = folder.listFiles();
            for(File file : listFiles) {
                if(file.isFile()) {
                    byte[] fileCodificado = readAll(file.getPath());
                    byte[] fileDecodificado = decodifica(fileCodificado);
                    writeBackupDecodificado(fileDecodificado, file.getName(), option);

                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void createBackup() {
        try {
            File folder = new File("dados");
            File[] listFiles = folder.listFiles();
            for(File file : listFiles) {
                if(file.isFile()) {
                    if(file.getName().endsWith(".db")) {
                        byte[] fileBytes = readAll(file.getPath());
                        byte[] fileCodificado = codifica(fileBytes);
                        writeBackup(fileCodificado, file.getName());
                        System.out.println(file.getName() + " original: " + fileBytes.length);
                        System.out.println(file.getName() + " codificado: " + fileCodificado.length);
                        System.out.println("-----------------------------------------------");
                    }                    
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] readAll(String nomeArquivo) {
        try {
            RandomAccessFile arq = new RandomAccessFile(nomeArquivo, "r");
            long size = arq.length();
            byte[] ba = new byte[(int) size];
            arq.read(ba);
            arq.close();
            return ba;            
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public static void writeBackup(byte[] ba, String nomeArquivo) {
        try {
            RandomAccessFile arq = new RandomAccessFile("backup_versao" + versao + "/"+ nomeArquivo, "rw");
            arq.seek(0);
            arq.write(ba);
            arq.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeBackupDecodificado(byte[] ba, String nomeArquivo, int option) {
        try {
            RandomAccessFile arq = new RandomAccessFile("backup_versao" + option + "_decodificado/"+ nomeArquivo, "rw");
            arq.seek(0);
            arq.write(ba);
            arq.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] codifica(byte[] msgBytes) throws Exception {

        ArrayList<ArrayList<Byte>> dicionario = new ArrayList<>(); // dicionario
        ArrayList<Byte> vetorBytes;  // auxiliar para cada elemento do dicionario
        ArrayList<Integer> saida = new ArrayList<>();

        // inicializa o dicionário
        byte b;
        for(int j=-128; j<128; j++) {
            b = (byte)j;
            vetorBytes = new ArrayList<>();
            vetorBytes.add(b);
            dicionario.add(vetorBytes);
        }

        int i=0;
        int indice=-1;
        int ultimoIndice;
        while(indice==-1 && i<msgBytes.length) { // testa se o último vetor de bytes não parou no meio caminho por falta de bytes
            vetorBytes = new ArrayList<>();
            b = msgBytes[i];
            vetorBytes.add(b);
            indice = dicionario.indexOf(vetorBytes);
            ultimoIndice = indice;

            while(indice!=-1 && i<msgBytes.length-1) {
                i++;
                b = msgBytes[i];
                vetorBytes.add(b);
                ultimoIndice = indice;
                indice = dicionario.indexOf(vetorBytes);

            }

            // acrescenta o último índice à saída
            saida.add(indice!=-1 ? indice : ultimoIndice);

            // acrescenta o novo vetor de bytes ao dicionário
            if(dicionario.size() < (Math.pow(2, BITS_POR_INDICE))) {
                dicionario.add(vetorBytes);
            }

        }

        System.out.println("Indices");
        System.out.println(saida);
        System.out.println("Dicionário tem "+dicionario.size()+" elementos");
        
        BitSequence bs = new BitSequence(BITS_POR_INDICE);
        for(i=0; i<saida.size(); i++) {
            bs.add(saida.get(i));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(bs.size());
        dos.write(bs.getBytes());
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static byte[] decodifica(byte[] msgCodificada) throws Exception {

        ByteArrayInputStream bais = new ByteArrayInputStream(msgCodificada);
        DataInputStream dis = new DataInputStream(bais);
        int n = dis.readInt();
        byte[] bytes = new byte[msgCodificada.length-4];
        dis.read(bytes);
        BitSequence bs = new BitSequence(BITS_POR_INDICE);
        bs.setBytes(n, bytes);

        // Recupera os números do bitset
        ArrayList<Integer>  entrada = new ArrayList<>();
        int i, j;
        for(i=0; i<bs.size(); i++) {
            j = bs.get(i);
        entrada.add(j);
        }

        // inicializa o dicionário
        ArrayList<ArrayList<Byte>> dicionario = new ArrayList<>(); // dicionario
        ArrayList<Byte> vetorBytes;  // auxiliar para cada elemento do dicionario
        byte b;
        for(j=-128; j<128; j++) {
            b = (byte)j;
            vetorBytes = new ArrayList<>();
            vetorBytes.add(b);
            dicionario.add(vetorBytes);
        }

        // Decodifica os números
        ArrayList<Byte> proximoVetorBytes;
        ArrayList<Byte> msgDecodificada = new ArrayList<>();
        i = 0;
        while( i< entrada.size() ) {

            // decodifica o número
            vetorBytes = (ArrayList<Byte>)(dicionario.get(entrada.get(i)).clone());
            msgDecodificada.addAll(vetorBytes);

            // decodifica o próximo número
            i++;
            if(i<entrada.size()) {
                // adiciona o vetor de bytes (+1 byte do próximo vetor) ao fim do dicionário
                if(dicionario.size()<Math.pow(2,BITS_POR_INDICE))
                    dicionario.add(vetorBytes);
                
                proximoVetorBytes = dicionario.get(entrada.get(i));
                vetorBytes.add(proximoVetorBytes.get(0));

            }

        }

        byte[] msgDecodificadaBytes = new byte[msgDecodificada.size()];
        for(i=0; i<msgDecodificada.size(); i++)
            msgDecodificadaBytes[i] = msgDecodificada.get(i);
        return msgDecodificadaBytes;

    }

    public static int getIndiceAtual() throws Exception{
        RandomAccessFile arq = new RandomAccessFile("indice/indice.txt ", "r");
        int indice = arq.readInt();
        arq.close();
        return indice;
    }

    public static void setIndiceAtual() throws Exception{
        RandomAccessFile arq = new RandomAccessFile("indice/indice.txt ", "rw");
        if(arq.length() == 0) {
            arq.writeInt(1);
        }
        else{
          arq.writeInt(getIndiceAtual() + 1);  
        }        
        arq.close();
    }
}