package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.TaxaCartao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaxaCartaoRepository {

    private final ConexaoFonte fonte;

    public TaxaCartaoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<TaxaCartao> listar(boolean apenasAtivos) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, bandeira, tipo_cartao, parcelas_min, parcelas_max, taxa_percentual, taxa_fixa, adquirente, ativo, criado_em
                  FROM taxas_cartao""");
        if (apenasAtivos) {
            sql.append(" WHERE ativo = TRUE");
        }
        sql.append(" ORDER BY bandeira, tipo_cartao, parcelas_min");
        List<TaxaCartao> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql.toString());
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar taxas de cartão", e);
        }
        return lista;
    }

    public Optional<TaxaCartao> buscar(String bandeira, String tipoCartao, int parcelas) {
        String sql = """
                SELECT id, bandeira, tipo_cartao, parcelas_min, parcelas_max, taxa_percentual, taxa_fixa, adquirente, ativo, criado_em
                  FROM taxas_cartao
                 WHERE ativo = TRUE AND UPPER(bandeira) = UPPER(?) AND UPPER(tipo_cartao) = UPPER(?) AND parcelas >= parcelas_min AND parcelas <= parcelas_max
                 ORDER BY parcelas_max DESC
                 LIMIT 1""";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, bandeira == null ? "" : bandeira);
            preparada.setString(2, tipoCartao == null ? "DEBITO" : tipoCartao);
            preparada.setInt(3, parcelas);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar taxa de cartão", e);
        }
        return Optional.empty();
    }

    public TaxaCartao salvar(TaxaCartao taxa) {
        String sql = taxa.getId() == null ? inserir() : atualizar();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(preparada, taxa);
            if (taxa.getId() != null) {
                preparada.setLong(9, taxa.getId());
            }
            preparada.executeUpdate();
            if (taxa.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        taxa.setId(chaves.getLong(1));
                    }
                }
            }
            return taxa;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar taxa de cartão", e);
        }
    }

    private String inserir() {
        return "INSERT INTO taxas_cartao (bandeira, tipo_cartao, parcelas_min, parcelas_max, taxa_percentual, taxa_fixa, adquirente, ativo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private String atualizar() {
        return "UPDATE taxas_cartao SET bandeira = ?, tipo_cartao = ?, parcelas_min = ?, parcelas_max = ?, taxa_percentual = ?, taxa_fixa = ?, adquirente = ?, ativo = ? WHERE id = ?";
    }

    private void preencher(PreparedStatement preparada, TaxaCartao taxa) throws SQLException {
        preparada.setString(1, taxa.getBandeira() == null ? "" : taxa.getBandeira().toUpperCase());
        preparada.setString(2, taxa.getTipoCartao() == null ? "DEBITO" : taxa.getTipoCartao().toUpperCase());
        preparada.setInt(3, Math.max(1, taxa.getParcelasMin()));
        preparada.setInt(4, Math.max(taxa.getParcelasMin(), taxa.getParcelasMax()));
        preparada.setBigDecimal(5, taxa.getTaxaPercentual());
        preparada.setBigDecimal(6, taxa.getTaxaFixa());
        if (taxa.getAdquirente() == null || taxa.getAdquirente().isBlank()) {
            preparada.setNull(7, Types.VARCHAR);
        } else {
            preparada.setString(7, taxa.getAdquirente());
        }
        preparada.setBoolean(8, taxa.isAtivo());
    }

    private TaxaCartao mapear(ResultSet resultado) throws SQLException {
        TaxaCartao taxa = new TaxaCartao();
        taxa.setId(resultado.getLong("id"));
        taxa.setBandeira(resultado.getString("bandeira"));
        taxa.setTipoCartao(resultado.getString("tipo_cartao"));
        taxa.setParcelasMin(resultado.getInt("parcelas_min"));
        taxa.setParcelasMax(resultado.getInt("parcelas_max"));
        taxa.setTaxaPercentual(resultado.getBigDecimal("taxa_percentual"));
        taxa.setTaxaFixa(resultado.getBigDecimal("taxa_fixa"));
        taxa.setAdquirente(resultado.getString("adquirente"));
        taxa.setAtivo(resultado.getBoolean("ativo"));
        Timestamp criado = resultado.getTimestamp("criado_em");
        taxa.setCriadoEm(criado == null ? null : criado.toLocalDateTime());
        return taxa;
    }
}
