package arquivos;

import aeds3.Arquivo;
import aeds3.ArvoreBMais;
import aeds3.HashExtensivel;
import aeds3.ParIntInt;
import aeds3.ListaInvertida;
import entidades.Livro;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ArquivoLivros extends Arquivo<Livro> {

  HashExtensivel<ParIsbnId> indiceIndiretoISBN;
  ArvoreBMais<ParIntInt> relLivrosDaCategoria;
  ListaInvertida lista;

  public ArquivoLivros() throws Exception {
    super("livros", Livro.class.getConstructor());
    indiceIndiretoISBN = new HashExtensivel<>(
        ParIsbnId.class.getConstructor(),
        4,
        "dados/livros_isbn.hash_d.db",
        "dados/livros_isbn.hash_c.db");
    relLivrosDaCategoria = new ArvoreBMais<>(ParIntInt.class.getConstructor(), 4, "dados/livros_categorias.btree.db");
    lista = new ListaInvertida(4, "dados/dicionario.listainv.db", "dados/blocos.listainv.db");

  }

  @Override
  public int create(Livro obj) throws Exception {
    int id = super.create(obj);
    obj.setID(id);
    String chave = obj.getTitulo();
    String[] chaves = processaStrings(chave);
    for (int i = 0; i < chaves.length; i++) {
      if (chaves[i] != null)
        lista.create(chaves[i], id);
    }

    indiceIndiretoISBN.create(new ParIsbnId(obj.getIsbn(), obj.getID()));
    relLivrosDaCategoria.create(new ParIntInt(obj.getIdCategoria(), obj.getID()));
    return id;
  }

  public Livro readISBN(String isbn) throws Exception {
    ParIsbnId pii = indiceIndiretoISBN.read(ParIsbnId.hashIsbn(isbn));
    if (pii == null)
      return null;
    int id = pii.getId();
    return super.read(id);
  }

  @Override
  public boolean delete(int id) throws Exception {
    Livro obj = super.read(id);
    String titulo = obj.getTitulo();
    if (obj != null) {
      

      if (indiceIndiretoISBN.delete(ParIsbnId.hashIsbn(obj.getIsbn()))
          &&
          relLivrosDaCategoria.delete(new ParIntInt(obj.getIdCategoria(), obj.getID())))
        return super.delete(id);
    }
    return false;
  }

  @Override
  public boolean update(Livro novoLivro) throws Exception {
    Livro livroAntigo = super.read(novoLivro.getID());
    if (livroAntigo != null) {

      // Testa alteração do ISBN
      if (livroAntigo.getIsbn().compareTo(novoLivro.getIsbn()) != 0) {
        indiceIndiretoISBN.delete(ParIsbnId.hashIsbn(livroAntigo.getIsbn()));
        indiceIndiretoISBN.create(new ParIsbnId(novoLivro.getIsbn(), novoLivro.getID()));
      }

      // Testa alteração da categoria
      if (livroAntigo.getIdCategoria() != novoLivro.getIdCategoria()) {
        relLivrosDaCategoria.delete(new ParIntInt(livroAntigo.getIdCategoria(), livroAntigo.getID()));
        relLivrosDaCategoria.create(new ParIntInt(novoLivro.getIdCategoria(), novoLivro.getID()));
      }

      // Atualiza o livro
      return super.update(novoLivro);
    }
    return false;
  }

  // Funcao de tratamento para os titulos

  public static String[] processaStrings(String chave) throws IOException {
    String[] chaves = chave.split(" ");
    for (int i = 0; i < chaves.length; i++) {
      chaves[i] = chaves[i].toLowerCase(); // Coloca em letras minusculas
      if (!isStopWord(chaves[i])) { // Checa se é uma stopword
        chaves[i] = removerAcentos(chaves[i]); // Se não for, termina de tratar tirando os acentos
      } else {
        chaves[i] = null; // Se for stopword, é retirada do array
      }
    }
    for (int i = 0; i < chaves.length; i++) {
      System.out.println(chaves[i]);
    }
    return chaves;
  }

  // Funcao que remove acento de palavras
private static final Map<Character, Character> MAPA_ACENTOS = new HashMap<>();

    static {
        MAPA_ACENTOS.put('á', 'a');
        MAPA_ACENTOS.put('é', 'e');
        MAPA_ACENTOS.put('í', 'i');
        MAPA_ACENTOS.put('ó', 'o');
        MAPA_ACENTOS.put('ú', 'u');
        MAPA_ACENTOS.put('â', 'a');
        MAPA_ACENTOS.put('ê', 'e');
        MAPA_ACENTOS.put('î', 'i');
        MAPA_ACENTOS.put('ô', 'o');
        MAPA_ACENTOS.put('û', 'u');
        MAPA_ACENTOS.put('ç', 'c');
        MAPA_ACENTOS.put('ã', 'a');
    }

    public static String removerAcentos(String texto) {
        System.out.println(texto + " entrou");
        StringBuilder textoSemAcento = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (MAPA_ACENTOS.containsKey(c)) {
                textoSemAcento.append(MAPA_ACENTOS.get(c));
            } else {
                textoSemAcento.append(c);
            }
        }
        return textoSemAcento.toString();
    }
  // Função itera por arquivo contendo todas as stopwords e checa se a palavra em
  // análise é ou não uma stopword
  public static boolean isStopWord(String s) throws IOException {
    boolean resp = false;
    BufferedReader br = new BufferedReader(new FileReader("dados/stopwords.txt"));
    String line;
    while ((line = br.readLine()) != null) {
      if (s.compareTo(line.trim()) == 0) { // .trim() usado pois arquivo possui espaço em branco extra ao final de cada linha
        resp = true;
        break;
      }
    }
    br.close();
    return resp;
  }


  public Livro[] buscar(String pesquisa) throws Exception {
    String [] chaves = processaStrings(pesquisa);
    int tam = chaves.length;
    int[] ids = lista.read(chaves[0]);
    int[] intersecao = new int[ids.length];
    
    if(ids.length == 0) {
      intersecao = new int[1];
      intersecao[0] = -1;
    }
    else {
      System.out.println("TESTE");
      for(int i = 1; i < tam; i++) {
        int[] ids_2 = lista.read(chaves[i]);
        intersecao = intersecao(ids, ids_2);
      }
    }

    Livro[] resultados = new Livro[intersecao.length];
    Livro controle = new Livro();
    resultados[0] = controle;

    if(intersecao[0] == -1) {
      return resultados;
    }

    for(int i = 0; i < intersecao.length; i++) {
      resultados[i] = super.read(intersecao[i]);
    }

    return resultados;
  }

  
  public int[] intersecao(int[] a, int[] b) {
    int[] intersecao = new int[a.length];
    intersecao[0] = -1;
    int k = 0;
    for(int i = 0; i < a.length; i++) {
      for(int j = 0; j < b.length; j++) {
        if(a[i] == b[j]) {
          intersecao[k] = a[i];
          k++;
        }
      }
    }
    return intersecao;
  }

}
