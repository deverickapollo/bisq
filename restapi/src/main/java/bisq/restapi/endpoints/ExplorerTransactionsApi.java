/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.restapi.endpoints;

import bisq.core.dao.state.model.blockchain.BaseTx;
import bisq.core.dao.state.model.blockchain.Tx;
import bisq.core.dao.state.model.blockchain.TxType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;



import bisq.restapi.DaoExplorerService;
import bisq.restapi.RestApi;
import bisq.restapi.RestApiMain;
import bisq.restapi.dto.JsonTx;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@Path("/explorer/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "TRANSACTIONS API")
public class ExplorerTransactionsApi {
    private final RestApi restApi;
    private final DaoExplorerService daoExplorerService;

    public ExplorerTransactionsApi(@Context Application application) {
        restApi = ((RestApiMain) application).getRestApi();
        daoExplorerService = restApi.getDaoExplorerService();
    }

    @GET
    @Path("get-bsq-tx/{txid}")
    public JsonTx getTx(@Parameter(description = "TxId")
                        @PathParam("txid") String txId) {
        Optional<JsonTx> jsonTx = restApi.getDaoStateService().getUnorderedTxStream()
                .filter(t -> t.getId().equals(txId))
                .map(this::getJsonTx)
                .findFirst();
        if (jsonTx.isPresent()) {
            log.info("supplying tx {} to client.", txId);
            return jsonTx.get();
        }
        log.warn("txid {} not found!", txId);
        return null;
    }

    @GET
    @Path("get-bsq-tx-for-addr/{addr}")
    public List<JsonTx> getBisqTxForAddr(@PathParam("addr") String address) {
        Map<String, Set<String>> addressToTxIds = daoExplorerService.getTxIdsByAddress();
        List<JsonTx> result = new ArrayList<>();
        Set<String> strings = addressToTxIds.get(address);
        strings.forEach(txId -> {
            restApi.getDaoStateService().getTx(txId).stream()
                    .map(this::getJsonTx)
                    .forEach(result::add);
        });
        log.info("getBisqTxForAddr: returning {} items.", result.size());
        return result;
    }

    @GET
    @Path("query-txs-paginated/{start}/{count}/{filters}")
    public List<JsonTx> queryTxsPaginated(@PathParam("start") int start,
                                          @PathParam("count") int count,
                                          @PathParam("filters") String filters) {
        log.info("filters: {}", filters);
        List<JsonTx> jsonTxs = restApi.getDaoStateService().getUnorderedTxStream()
                .sorted(Comparator.comparing(BaseTx::getTime).reversed())
                .filter(tx -> hasMatchingTxType(tx, filters))
                .skip(start)
                .limit(count)
                .map(this::getJsonTx)
                .collect(Collectors.toList());
        log.info("supplying {} jsonTxs to client from index {}", jsonTxs.size(), start);
        return jsonTxs;
    }

    private boolean hasMatchingTxType(Tx tx, String filters) {
        String[] filterTokens = filters.split("~");
        if (filterTokens.length < 1 || filters.equalsIgnoreCase("~")) {
            return true;
        }
        for (String filter : filterTokens) {
            try {
                TxType txType = Enum.valueOf(TxType.class, filter);
                if (tx.getTxType() == txType) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Could not resolve TxType Enum from " + filter, e);
                return false;
            }
        }
        return false;
    }

    private JsonTx getJsonTx(Tx tx) {
        return daoExplorerService.getJsonTx(tx);
    }
}
