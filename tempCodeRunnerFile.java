byte[] msgCodificada = codifica(msgBytes);

            System.out.println(msg);
            System.out.println("mensagem original tem "+msgBytes.length+" bytes");
            System.out.println("codificado em "+msgCodificada.length+" bytes");

            byte[] msgDecodificada = decodifica(msgCodificada);
            System.out.println(new String(msgDecodificada));