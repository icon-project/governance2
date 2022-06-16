Governance SCORE APIs
=====================

This document describes APIs that Governance SCORE provides.

# Overview

* Governance SCORE is a built-in SCORE that manages adjustable characteristics of ICON network.
* Address: `cx0000000000000000000000000000000000000001`

# Value Types

By default, Values in all JSON-RPC messages are in string form.
The most commonly used Value types are as follows.

| Value Type                              | Description                                                       | Example                                                                                  |
|:----------------------------------------|:------------------------------------------------------------------|:-----------------------------------------------------------------------------------------|
| <a id="T_ADDR_EOA">T\_ADDR\_EOA</a>     | "hx" + 40 digits HEX string                                       | hxbe258ceb872e08851f1f59694dac2558708ece11                                               |
| <a id="T_ADDR_SCORE">T\_ADDR\_SCORE</a> | "cx" + 40 digits HEX string                                       | cxb0776ee37f5b45bfaea8cff1d8232fbb6122ec32                                               |
| <a id="T_ADDR">T\_ADDR</a>              | T_ADDR_EOA + T_ADDR_SCORE                                         |                                                                                          |
| <a id="T_HASH">T\_HASH</a>              | "0x" + 64 digits HEX string                                       | 0xc71303ef8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238                       |
| <a id="T_INT">T\_INT</a>                | "0x" + lowercase HEX string                                       | 0xa                                                                                      |
| <a id="T_STR">T\_STR</a>                | string                                                            | hello                                                                                    |
| <a id="T_BIN_DATA">T\_BIN\_DATA</a>     | "0x" + lowercase HEX string (the length of string should be even) | 0x34b2                                                                                   |
| <a id="T_SIG">T\_SIG</a>                | base64 encoded string                                             | VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA= |
| <a id="T_LIST">T\_LIST</a>              | list                                                              | \["a", "b", "0x1"\]                                                                      |
| <a id="T_DICT">T\_DICT</a>              | dict                                                              | {"key": "value"}                                                                         |


# Methods List

* Query methods
  * [getScoreStatus](#getscorestatus)
  * [getStepPrice](#getstepprice)
  * [getStepCosts](#getstepcosts)
  * [getMaxStepLimit](#getmaxsteplimit)
  * [isInScoreBlackList](#isinscoreblacklist)
  * [getVersion](#getversion)
  * [getRevision](#getrevision)
  * [getProposal](#getproposal)
  * [getProposals](#getproposals)
* Invoke methods
  * [acceptScore](#acceptscore)
  * [rejectScore](#rejectscore)
  * [addAuditor](#addauditor)
  * [removeAuditor](#removeauditor)
  * [registerProposal](#registerproposal)
  * [cancelProposal](#cancelproposal)
  * [voteProposal](#voteproposal)
  * [applyProposal](#applyproposal)
* Eventlog
  * [Accepted](#accepted)
  * [Rejected](#rejected)
  * [StepPriceChanged](#steppricechanged)
  * [StepCostChanged](#stepcostchanged)
  * [RevisionChanged](#revisionchanged)
  * [MaliciousScore](#maliciousscore-event)
  * [PRepDisqualified](#prepdisqualified)
  * [NetworkProposalRegistered](#networkproposalregistered)
  * [NetworkProposalCanceled](#networkproposalcanceled)
  * [NetworkProposalVoted](#networkproposalvoted)
  * [NetworkProposalApproved](#networkproposalapproved)
  * [NetworkProposalDisapproved](#networkproposaldisapproved)
  * [NetworkProposalApplied](#networkproposalapplied)
  * [NetworkProposalExpired](#networkproposalexpired)

# Query Methods

Query method does not change state. Read-only.

## getScoreStatus

* Queries the current status of the given SCORE.
* `current` indicates the currently active SCORE instance, while `next` is the SCORE code that has been requested to install or update, but not activated yet.
* [Fee 2.0] Checks the deposit information of the given SCORE.

### Parameters

| Key     | Value Type                      | Description                           |
|:--------|:--------------------------------|---------------------------------------|
| address | [T\_ADDR\_SCORE](#T_ADDR_SCORE) | SCORE address whose status be checked |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "to": "cx0000000000000000000000000000000000000001",
    "dataType": "call",
    "data": {
      "method": "getScoreStatus",
      "params": {
        "address": "cxb0776ee37f5b45bfaea8cff1d8232fbb6122ec32"
      }
    }
  }
}
```

#### Response

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": {
    "current": {
      "status": "active",
      "deployTxHash": "0x19793f41b8e64fc31190c6a70a103103da1f4bc81bc829fa72c852a5e388fe8c"
    },
    "depositInfo": {
      "scoreAddress": "cx216e1468b780ac1b54c328d19ea23a35a6899e55",
      "availableVirtualStep": "0x9502c665a",
      "availableDeposit": "0xf3f20b8dfa69d00000"
    }
  }
}
```

#### Response: error case

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "error": {
    "code": -32032,
    "message": "SCORE not found"
  }
}
```

## getStepPrice

* Returns the current step price in loop.

### Parameters

None

### Returns

`T_INT` - integer of the current step price in loop (1 ICX == 10^18 loop).

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "to": "cx0000000000000000000000000000000000000001",
    "dataType": "call",
    "data": {
      "method": "getStepPrice"
    }
  }
}
```

#### Response

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": "0x2540be400"
}
```

## getStepCosts

* Returns a table of the step costs for each actions.

### Parameters

None

### Returns

`T_DICT` - a dict:  key - camel-cased action strings, value - step costs in integer

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "to": "cx0000000000000000000000000000000000000001",
    "dataType": "call",
    "data": {
      "method": "getStepCosts"
    }
  }
}
```

#### Response

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": {
    "default": "0x186a0",
    "contractCall": "0x61a8",
    "contractCreate": "0x3b9aca00",
    "contractUpdate": "0x5f5e1000",
    "contractDestruct": "-0x11170",
    "contractSet": "0x7530",
    "get": "0x0",
    "set": "0x140",
    "replace": "0x50",
    "delete": "-0xf0",
    "input": "0xc8",
    "eventLog": "0x64",
    "apiCall": "0x2710"
  }
}
```

## getMaxStepLimit

* Returns the maximum step limit value that any SCORE execution should be bounded by.

### Parameters

| Key         | Value Type | Description                                    |
|:------------|:-----------|------------------------------------------------|
| contextType | string     | 'invoke' for sendTransaction, 'query' for call |

### Returns

`T_INT` - integer of the maximum step limit

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "to": "cx0000000000000000000000000000000000000001",
    "dataType": "call",
    "data": {
      "method": "getMaxStepLimit",
      "params": {
        "contextType": "invoke"
      }
    }
  }
}
```

#### Response

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": "0x9502f900"
}
```

## isInScoreBlackList

* Returns "0x1" if the given address is in the deployer list.

### Parameters

| Key     | Value Type                      | Description            |
|:--------|:--------------------------------|------------------------|
| address | [T\_ADDR\_SCORE](#T_ADDR_SCORE) | SCORE address to query |

### Returns

`T_INT` - "0x1" if the SCORE address is in the black list, otherwise "0x0"

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "to": "cx0000000000000000000000000000000000000001",
    "dataType": "call",
    "data": {
      "method": "isInScoreBlackList",
      "params": {
        "address": "cxb0776ee37f5b45bfaea8cff1d8232fbb6122ec32"
      }
    }
  }
}
```

#### Response

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": "0x1"
}
```



## getVersion

- Returns the version of Governance SCORE

### Returns

`T_STR` - version string

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "to": "cx0000000000000000000000000000000000000001",
    "dataType": "call",
    "data": {
      "method": "getVersion"
    }
  }
}
```

#### Response

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": "0.0.7"
}
```


## getRevision

* Returns info about revision.

### Parameters

None

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "to": "cx0000000000000000000000000000000000000001",
    "dataType": "call",
    "data": {
      "method": "getRevision"
    }
  }
}
```

#### Response

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": {
    "code": "0xf",
    "name": "1.3.0"
  }
}
```

## getProposal

* Query information about the network proposal.

### Parameters

| Key  | Value Type         | Description                                         |
|:-----|:-------------------|-----------------------------------------------------|
| id   | [T\_HASH](#T_HASH) | Transaction hash of the registered network proposal |

### Returns

`T_DICT` - Information of the network proposal in dict

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "version": "0x3",
    "from": "hx8f21e5c54f006b6a5d5fe65486908592151a7c57",
    "to": "cx0000000000000000000000000000000000000001",
    "timestamp": "0x563a6cf330136",
    "dataType": "call",
    "data": {
      "method": "getProposal",
      "params": {
        "id": "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238"
      }
    }
  }
}
```

#### Response
Proposal registered in governance2.0.0 has different response format from previously registered proposal.
The following shows the proposal registered before governance2 and the proposal registered in governance2.  
Check [changes](https://github.com/icon-project/governance2/blob/main/doc/network_proposal_changes.md)

#### Response1
registered in governance2
```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": {
    "id" : "0xb903239f8543d0..",
    "proposer" : "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "proposerName" : "P-Rep A",
    "status" : "0x0",
    "startBlockHeight" : "0x1",
    "endBlockHeight" : "0x65",
    "vote": {
      "agree": {
        "list":[{
          "id": "0xb903239f854..",
          "timestamp": "0x563a6cf330136",
          "address": "hxe7af5fcfd8dfc67530a01a0e403882687528dfcb",
          "name": "P-Rep B",
          "amount": "0x1"
        }, .. ],
        "amount": "0x12345"
      },
      "disagree": {
        "list": [{
          "id": "0xa803239f854..",
          "timestamp": "0x563a6cf330136",
          "address": "hxbe258ceb872e08851f1f59694dac2558708ece11",
          "name": "P-Rep C",
          "amount": "0x1"
        }, .. ],
        "amount": "0x123"
      },
      "noVote": {
        "list": ["hx31258ceb872e08851f1f59694dac2558708ece11", .. , "hx31258ceb872e08851f1f59694dac2558708eceff"],
        "amount": "0x12312341234a"
      },
    },
    "contents": {
      "title": "set revision",
      "description": "set revision 18",
      "type":"0x9",
      "value": {
        "data": "[{\"value\": {\"revision\": \"0x12\"}}]"
      }
    }
  }
}
```

#### Response2
registered before governance2
```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": {
    "id" : "0xb903239f8543d0..",
    "proposer" : "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "proposerName" : "P-Rep A",
    "status" : "0x0",
    "startBlockHeight" : "0x1",
    "endBlockHeight" : "0x65",
    "vote": {
      "agree": {
        "list":[{
          "id": "0xb903239f854..",
          "timestamp": "0x563a6cf330136",
          "address": "hxe7af5fcfd8dfc67530a01a0e403882687528dfcb",
          "name": "P-Rep B",
          "amount": "0x1"
        }, .. ],
        "amount": "0x12345"
      },
      "disagree": {
        "list": [{
          "id": "0xa803239f854..",
          "timestamp": "0x563a6cf330136",
          "address": "hxbe258ceb872e08851f1f59694dac2558708ece11",
          "name": "P-Rep C",
          "amount": "0x1"
        }, .. ],
        "amount": "0x123"
      },
      "noVote": {
        "list": ["hx31258ceb872e08851f1f59694dac2558708ece11", .. , "hx31258ceb872e08851f1f59694dac2558708eceff"],
        "amount": "0x12312341234a"
      },
    },
    "contents": {
      "title": "set revision",
      "description": "set revision 14",
      "type": "0x1",
      "value": {
        "revision": "0xe"
      }
    }
  }
}
```

## getProposals

* Query network proposals.

### Parameters

| Key    | Value Type       | Description                                                                  |
|:-------|:-----------------|------------------------------------------------------------------------------|
| type   | [T\_INT](#T_INT) | Type for querying (optional)                                                 |
| status | [T\_INT](#T_INT) | Status for querying (optional)                                               |
| start  | [T\_INT](#T_INT) | Starting index for querying. Default is 0, which means the latest (optional) |
| size   | [T\_INT](#T_INT) | Size for querying. Default and maximum is 10 (optional)                      |

### Returns

`T_LIST` - List of summarized information of network proposals in dict.

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_call",
  "params": {
    "version": "0x3",
    "from": "hx8f21e5c54f006b6a5d5fe65486908592151a7c57",
    "to": "cx0000000000000000000000000000000000000001",
    "timestamp": "0x563a6cf330136",
    "dataType": "call",
    "data": {
      "method": "getProposals",
      "params": {
        "type": "0x3",
        "status": "0x0",
        "start": "0x1",
        "size": "0x3"
      }
    }
  }
}
```

#### Response

```json
{
  "jsonrpc":"2.0",
  "id":1234,
  "result":{
    "proposals":[
      {
        "id":"0xb903239f8543..",
        "proposer":"hxbe258ceb872e08851f1f59694dac2558708ece11",
        "proposerName":"P-Rep A",
        "status":"0x0",
        "startBlockHeight":"0x1",
        "endBlockHeight":"0x65",
        "vote":{
          "agree":{
            "count":"0x1",
            "amount":"0x12312341234a"
          },
          "disagree":{
            "count":"0x1",
            "amount":"0x12312341234a"
          },
          "noVote":{
            "count":"0x1",
            "amount":"0x12312341234a"
          }
        },
        "contents":{
          "title": "set revision",
          "description": "set revision 18",
          "type":"0x9",
          "value": {
            "data": "[{\"value\": {\"revision\": \"0x12\"}}]"
          }
        }
      }, .. ]
  }
}
```


# Invoke Methods

Invoke method can initiate state transition.

## acceptScore

* Accepts SCORE deployment request.
* This method can be invoked only from the addresses that are in the auditor list.
* The accepted SCORE will be executing from the next block.

### Parameters

| Key    | Value Type         | Description                                       |
|:-------|:-------------------|---------------------------------------------------|
| txHash | [T\_HASH](#T_HASH) | Transaction hash of the SCORE deploy transaction. |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 1234,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x12345",
    "timestamp": "0x563a6cf330136",
    "nonce": "0x1",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA=",
    "dataType": "call",
    "data": {
      "method": "acceptScore",
      "params": {
        "txHash": "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238"
      }
    }
  }
}
```

## rejectScore

* Rejects SCORE deployment request.
* This can be invoked only from the addresses that are in the auditor list.

### Parameters

| Key    | Value Type         | Description                                   |
|:-------|:-------------------|-----------------------------------------------|
| txHash | [T\_HASH](#T_HASH) | Transaction hash of the SCORE deploy request. |
| reason | T\_TEXT            | Reason for rejecting                          |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 1234,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x12345",
    "timestamp": "0x563a6cf330136",
    "nonce": "0x1",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA=",
    "dataType": "call",
    "data": {
      "method": "rejectScore",
      "params": {
        "txHash": "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238",
        "reason": "SCORE cannot use network api"
      }
    }
  }
}
```

## addAuditor

* Adds a new address to the auditor list.
* Only the addresses registered in the auditor list can call `acceptScore` and `rejectScore`.
* Only the owner of the Governance SCORE can call this function.

### Parameters

| Key     | Value Type                  | Description                                            |
|:--------|:----------------------------|--------------------------------------------------------|
| address | [T\_ADDR\_EOA](#T_ADDR_EOA) | New EOA address that will be added to the auditor list |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x30000",
    "timestamp": "0x563a6cf330136",
    "nonce": "0x1",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA=",
    "dataType": "call",
    "data": {
      "method": "addAuditor",
      "params": {
        "address": "hx2d54d5ca2a1dffbcfc3fb2c86cc07cb826f6b931"
      }
    }
  }
}
```

## removeAuditor

* Removes the address from the auditor list.
* The address removed from the auditor list cannot call `acceptScore` and `rejectScore` afterward.
* This function can be invoked only by either Governance SCORE owner or the auditor herself.

### Parameters

| Key     | Value Type                  | Description                     |
|:--------|:----------------------------|---------------------------------|
| address | [T\_ADDR\_EOA](#T_ADDR_EOA) | EOA address in the auditor list |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x30000",
    "timestamp": "0x563a6cf330136",
    "nonce": "0x1",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA=",
    "dataType": "call",
    "data": {
      "method": "removeAuditor",
      "params": {
        "address": "hx2d54d5ca2a1dffbcfc3fb2c86cc07cb826f6b931"
      }
    }
  }
}
```

## registerProposal

* Registers diverse network proposals in a transaction
* This function can be invoked only by main P-Reps at that time.

### Parameters

| Key         | Value Type       | Description                                                                                                                                                |
|:------------|:-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| title       | [T\_STR](#T_STR) | Title of the network proposal                                                                                                                              |
| description | [T\_STR](#T_STR) | Description of the network proposal                                                                                                                        |
| value       | T\_LIST[T\_DICT] | Values for each type of network proposal. Hex string of UTF-8 encoded bytes data of JSON string<br />ex. "0x" + bytes.hex(json.dumps(value_list).encode()) |

#### Value format for each type

* value contains the list of network proposals below.
* Proposals are applied in the order they are included in `value` list.
 
##### text

* Stores the contents of the proposal in the blockchain.
* It does not affect any blockchain behavior.

| Key        | Value Type        | Description          |
|:-----------|:------------------|----------------------|
| name       | [T\_STR](#T_STR)  | "text" (fixed value) |
| value      | T\_DICT           |                      |
| value.text | [T\_STR](#T_STR)  | Text value           |

```json
{
  "name": "text",
  "value": {
    "text": "This is a text proposal"
  }
}
```

##### revision

* Revision update proposal

| Key            | Value Type        | Description              |
|:---------------|:------------------|--------------------------|
| name           | [T\_STR](#T_STR)  | "revision" (fixed value) |
| value          | T\_DICT           |                          |
| value.revision | [T\_INT](#T_INT)  | Revision code            |

```json
{
  "name": "revision",
  "value": {
    "revision": "0x11"
  }
}
```

##### maliciousScore

* Controls access to a given score

| Key           | Value Type                      | Description                    |
|:--------------|:--------------------------------|--------------------------------|
| name          | [T\_STR](#T_STR)                | "maliciousScore" (fixed value) |
| value         | T\_DICT                         |                                |
| value.address | [T\_ADDR\_SCORE](#T_ADDR_SCORE) | SCORE address                  |
| value.type    | [T\_INT](#T_INT)                | 0x0: Freeze, 0x1: Unfreeze     |

```json
{
  "name": "maliciousScore",
  "value": {
    "address": "cx7cc546bf908018b5602b66fa65ff5fdacef45fe0",
    "type": "0x0"
  }
}
```

##### prepDisqualification

* Disqualifies a given P-Rep

| Key           | Value Type                  | Description                          |
|:--------------|:----------------------------|--------------------------------------|
| name          | [T\_STR](#T_STR)            | "prepDisqualification" (fixed value) |
| value         | T\_DICT                     |                                      |
| value.address | [T\_ADDR\_EOA](#T_ADDR_EOA) | EOA address of main/sub P-Rep        |

```json
{
  "name": "prepDisqualification",
  "value": {
    "address": "hx7cc546bf908018b5602b66fa65ff5fdacef45fe0"
  }
}
```

##### stepPrice

* Updates step price

| Key             | Value Type       | Description                          |
|:----------------|:-----------------|--------------------------------------|
| name            | [T\_STR](#T_STR) | "stepPrice" (fixed value)            |
| value           | T\_DICT          |                                      |
| value.stepPrice | [T\_INT](#T_INT) | An integer of the step price in loop |

```json
{
  "name": "stepPrice",
  "value": {
    "stepPrice": "0x2e90edd00"
  }
}
```

##### stepCosts

* Updates given step costs.
* All fields below is optional.

| Key         | Value Type         | Description                                                                                           |
|:------------|:-------------------|-------------------------------------------------------------------------------------------------------|
| name        | [T\_STR](#T_STR)   | "stepCosts" (fixed value)                                                                             |
| value       | T\_DICT            |                                                                                                       |
| value.costs | [T\_DICT](#T_DICT) | Step costs to set as a dict. <br> All fields are optional but at least one field should be specified. |

| Key            | Value Type       | Description                                                      |
|:---------------|:-----------------|------------------------------------------------------------------|
| schema         | [T\_INT](#T_INT) | Schema version (currently fixed at 1)                            |
| default        | [T\_INT](#T_INT) | Default cost charged each time transaction is executed           |
| contractCall   | [T\_INT](#T_INT) | Cost to call the smart contract function                         |
| contractCreate | [T\_INT](#T_INT) | Cost to call the smart contract code generation function         |
| contractUpdate | [T\_INT](#T_INT) | Cost to call the smart contract code update function             |
| contractSet    | [T\_INT](#T_INT) | Cost to store the generated/updated smart contract code per byte |
| get            | [T\_INT](#T_INT) | Cost to get values from the state database per byte              |
| getBase        | [T\_INT](#T_INT) | Default cost charged each time `get` is called                   |
| set            | [T\_INT](#T_INT) | Cost to set values newly in the state database per byte          |
| setBase        | [T\_INT](#T_INT) | Default cost charged each time `set` is called                   |
| delete         | [T\_INT](#T_INT) | Cost to delete values in the state database per byte             |
| deleteBase     | [T\_INT](#T_INT) | Default cost charged each time `delete` is called                |
| input          | [T\_INT](#T_INT) | Cost charged for input data included in transaction per byte     |
| log            | [T\_INT](#T_INT) | Cost to emit event logs per byte                                 |
| logBase        | [T\_INT](#T_INT) | Default cost charged each time `log` is called                   |
| apiCall        | [T\_INT](#T_INT) | Cost charged for heavy API calls (e.g. hash functions)           |

```json
{
  "name": "stepCosts",
  "value": {
    "costs": {"default": "0x186a0", "set": "0x140"}
  }
}
```

##### rewardFund

* Updates Iglobal value, which is the total amount of monthly reward fund in loop unit.

| Key           | Value Type       | Description                                     |
|:--------------|:-----------------|-------------------------------------------------|
| name          | [T\_STR](#T_STR) | "rewardFund" (fixed value)                      |
| value         | T\_DICT          |                                                 |
| value.iglobal | [T\_INT](#T_INT) | The total amount of monthly reward fund in loop |

```json
{
    "name": "rewardFund",
    "value": {
        "iglobal": "0x27b46536c66c8e3000000"
    }
}
```

##### rewardFundsAllocation

* Determines the allocation proportion of the monthly reward fund.
* Each allocation should be an integer in percentage. [0 ~ 100]
* The sum of all allocations must be 100.
* All allocations are required.

| Key               | Value Type         | Description                                                     |
|:------------------|:-------------------|-----------------------------------------------------------------|
| name              | [T\_STR](#T_STR)   | "rewardFundsAllocation" (fixed value)                           |
| value             | T\_DICT            |                                                                 |
| value.rewardFunds | [T\_DICT](#T_DICT) | Reward fund values information to set. All values are required. |

| Key    | Value Type       | Description                                                            |
|:-------|:-----------------|------------------------------------------------------------------------|
| iprep  | [T\_INT](#T_INT) | The percentage allocated to the P-Rep from the monthly reward fund     |
| icps   | [T\_INT](#T_INT) | The percentage allocated to the CPS from the monthly reward fund       |
| irelay | [T\_INT](#T_INT) | The percentage allocated to the BTP relay from the monthly reward fund |
| ivoter | [T\_INT](#T_INT) | The percentage allocated to the Voter from the monthly reward fund     |

```json
{
  "name": "rewardFundsAllocation",
  "value": {
    "rewardFunds": {
      "iprep": "0x10",
      "icps": "0xa",
      "irelay": "0xa",
      "ivoter": "0x40"
    }
  }
}
```

##### networkScoreDesignation

* Designates a given score to the network score which plays a specific role.
* The designated score gets the reward fund for its role from blockchain every term.
* The only scores owned by governance score can be designated to network scores.

| Key                         | Value Type                      | Description                                                                               |
|:----------------------------|:--------------------------------|-------------------------------------------------------------------------------------------|
| name                        | [T\_STR](#T_STR)                | "networkScoreDesignation" (fixed value)                                                   |
| value                       | T\_DICT                         |                                                                                           |
| value.networkScores         | T\_LIST[T\_DICT]                | network SCORE values to set. If the address is an empty string, deallocate network SCORE. |
| value.networkScores.role    | [T\_STR](#T_STR)                | "cps" or "relay"                                                                          |
| value.networkScores.address | [T\_ADDR\_SCORE](#T_ADDR_SCORE) | network SCORE address                                                                     |

```json
{
  "name": "networkScoreDesignation",
  "value": {
    "networkScores": [
      {
        "role": "cps",
        "address": "cx7cc546bf908018b5602b66fa65ff5fdacef45fe0"
      }
    ]
  }
}
```

##### networkScoreUpdate

* Updates a network score with a given address and content.
* Only java scores are supported.

| Key           | Value Type                      | Description                          |
|:--------------|:--------------------------------|--------------------------------------|
| name          | [T\_STR](#T_STR)                | "networkScoreUpdate" (fixed value)   |
| value         | T\_DICT                         |                                      |
| value.address | [T\_ADDR\_SCORE](#T_ADDR_SCORE) | network SCORE address to update      |
| value.content | [T_BIN_DATA](#T_BIN_DATA)       | SCORE code in hexadecimal string     |
| value.params  | [T_LIST](#T_LIST)               | Parameters passed to score on update |

```json
{
  "name": "networkScoreUpdate",
  "value": {
    "address": "cx7cc546bf908018b5602b66fa65ff5fdacef45fe0",
    "content": "0x504b0304107082bc2bf352a000000280...00000504b03041400080808000000210000000",
    "params": ["0x10", "Hello"]
  }
}
```

##### accumulatedValidationFailureSlashingRate

* Sets accumulated validation failure slashing rate.
* `SlashingRateChanged("ConsistentValidationPenalty",int)` eventLog is emitted on applying the proposal.

| Key                | Value Type       | Description                                              |
|:-------------------|:-----------------|----------------------------------------------------------|
| name               | [T\_STR](#T_STR) | "accumulatedValidationFailureSlashingRate" (fixed value) |
| value              | T\_DICT          |                                                          |
| value.slashingRate | [T\_INT](#T_INT) | slashing rate [0 ~ 100]                                  |

```json
{
  "name": "accumulatedValidationFailureSlashingRate",
  "value": {
    "slashingRate": "0x5"
  }
}
```

##### missedNetworkProposalVoteSlashingRate

* Sets missed network proposal vote slashing rate.

| Key                | Value Type       | Description                                           |
|:-------------------|:-----------------|-------------------------------------------------------|
| name               | [T\_STR](#T_STR) | "missedNetworkProposalVoteSlashingRate" (fixed value) |
| value              | T\_DICT          |                                                       |
| value.slashingRate | [T\_INT](#T_INT) | slashing rate [0 ~ 100]                               |

```json
{
  "name": "missedNetworkProposalVoteSlashingRate",
  "value": {
    "slashingRate": "0x5"
  }
}
```

##### openBTPNetwork

* Open BTP network.

| Key                   | Value Type         | Description                                      |
|:----------------------|:-------------------|--------------------------------------------------|
| name                  | [T\_STR](#T_STR)   | "openBTPNetwork" (fixed value)                   |
| value                 | T\_DICT            |                                                  |
| value.networkTypeName | [T\_STR](#T_STR)   | BTP network type name                            |
| value.name            | [T\_STR](#T_STR)   | BTP network name                                 |
| value.owner           | [T\_ADDR](#T_ADDR) | Owner of the BTP network who can send a message  |

```json
{
  "name": "openBTPNetwork",
  "value": {
    "networkTypeName": "eth",
    "name": "ethereum",
    "owner": "cxb0776ee37f5b45bfaea8cff1d8232fbb6122ec32"
  }
}
```

##### closeBTPNetwork

* Close BTP network.

| Key       | Value Type       | Description                     |
|:----------|:-----------------|---------------------------------|
| name      | [T\_STR](#T_STR) | "closeBTPNetwork" (fixed value) |
| value     | T\_DICT          |                                 |
| value.id  | [T\_INT](#T_INT) | BTP network ID                  |

```json
{
  "name": "closeBTPNetwork",
  "value": {
    "id": "0x1"
  }
}
```

### Examples

*Request*

```json
{
  "jsonrpc": "2.0",
  "id": 1234,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hx8f21e5c54f006b6a5d5fe65486908592151a7c57",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x12345",
    "timestamp": "0x563a6cf330136",
    "nid": "0x3",
    "nonce": "0x0",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m5...",
    "dataType": "call",
    "data": {
      "method": "registerProposal",
      "params": {
        "title": "Disqualify P-Rep A",
        "description": "P-Rep A does not maintain node",
        "value": "0x7b2261646472657373223a2022.."
      }
    }
  }
}
```

`value` is in hexadecimal format of the following json string.

```json
[
  {
    "name": "missedNetworkProposalVoteSlashingRate",
    "value": {
      "slashingRate": "0x5"
    }
  },
  {
    "name": "accumulatedValidationFailureSlashingRate",
    "value": {
      "slashingRate": "0x5"
    }
  },
  {
    "name": "rewardFundsAllocation",
    "value": {
      "rewardFunds": {
        "iprep": "0x10",
        "icps": "0xa",
        "irelay": "0xa",
        "ivoter": "0x40"
      }
    }
  },
  ...
]
```

## cancelProposal

* Cancel the network proposal

### Parameters

| Key | Value Type         | Description                                    |
|:----|:-------------------|------------------------------------------------|
| id  | [T\_HASH](#T_HASH) | Transaction hash of network proposal to cancel |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x30000",
    "timestamp": "0x563a6cf330136",
    "nonce": "0x1",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA=",
    "dataType": "call",
    "data": {
      "method": "cancelProposal",
      "params": {
        "id": "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238"
      }
    }
  }
}
```

## voteProposal

* vote on the network proposal

### Parameters

| Key  | Value Type         | Description                                  |
|:-----|:-------------------|----------------------------------------------|
| id   | [T\_HASH](#T_HASH) | Transaction hash of network proposal to vote |
| vote | [T\_INT](#T_INT)   | 0x0: Disagree, 0x1: Agree                    |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x30000",
    "timestamp": "0x563a6cf330136",
    "nonce": "0x1",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA=",
    "dataType": "call",
    "data": {
      "method": "voteProposal",
      "parmas": {
        "id" : "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238",
        "vote" : "0x1"
      }
    }
  }
}
```

## applyProposal

* apply proposal

### Parameters

| Key | Value Type         | Description                                   |
|:----|:-------------------|-----------------------------------------------|
| id  | [T\_HASH](#T_HASH) | Transaction hash of network proposal to apply |

### Examples

#### Request

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "icx_sendTransaction",
  "params": {
    "version": "0x3",
    "from": "hxbe258ceb872e08851f1f59694dac2558708ece11",
    "to": "cx0000000000000000000000000000000000000001",
    "stepLimit": "0x30000",
    "timestamp": "0x563a6cf330136",
    "nonce": "0x1",
    "signature": "VAia7YZ2Ji6igKWzjR2YsGa2m53nKPrfK7uXYW78QLE+ATehAVZPC40szvAiA6NEU5gCYB4c4qaQzqDh2ugcHgA=",
    "dataType": "call",
    "data": {
      "method": "applyProposal",
      "parmas": {
        "id" : "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238",
      }
    }
  }
}
```

# Eventlog

## Accepted

Triggered on any successful acceptScore transaction.

```java
@EventLog(indexed=1)
public void Accepted(byte[] txHash) {}
```

## Rejected

Triggered on any successful rejectScore transaction.

```java
@EventLog(indexed=1)
public void Rejected(byte[] txHash, String reason) {}
```

## StepPriceChanged

Triggered on vote transaction approving 'Step Price' network proposal.

```java
@EventLog(indexed=1)
public void StepPriceChanged(BigInteger stepPrice) {}
```

## StepCostChanged

Triggered on vote transaction approving 'Step Costs' network proposal.

```java
@EventLog(indexed=1)
public void StepCostChanged(String type, BigInteger stepCost) {}
```

## RevisionChanged

Triggered on vote transaction approving 'Revision' network proposal.

```java
@EventLog(indexed=0)
public void RevisionChanged(BigInteger revisionCode) {}
```

## MaliciousSCORE event

Triggered on vote transaction approving 'Malicious SCORE' network proposal.

```java
@EventLog(indexed=0)
public void MaliciousScore(Address address, int unFreeze) {}
```

## PRepDisqualified

Triggered on vote transaction approving 'P-Rep Disqualification' network proposal.

```java
@EventLog(indexed=0)
public void PRepDisqualified(Address address, boolean success, String reason) {}
```

## RewardFundSettingChanged

Triggered on vote transaction approving 'Reward Fund Setting' network proposal.

```java
@EventLog(indexed=0)
public void RewardFundSettingChanged(BigInteger rewardFund) {}
```

## RewardFundAllocationChanged

Triggered on vote transaction approving 'Reward Fund Allocation' network proposal.

```java
@EventLog(indexed=0)
public void RewardFundAllocationChanged(BigInteger iprep, BigInteger icps, BigInteger irelay, BigInteger ivoter) {}
```

## NetworkScoreUpdated

Triggered on vote transaction approving 'Network Score Update' network proposal.

```java
@EventLog(indexed=1)
public void NetworkScoreUpdated(Address address) {}
```

## NetworkScoreDesignated

Triggered on vote transaction approving 'Network Score Designation' network proposal.

```java
@EventLog(indexed=1)
public void NetworkScoreDesignated(String role, Address address) {}
```

## NetworkScoreDeallocated

Triggered on vote transaction approving 'Network Score Designation' network proposal.

```java
@EventLog(indexed=1)
public void NetworkScoreDeallocated(String role) {}
```

## NetworkProposalRegistered

Triggered on any successful registerProposal transaction.

```java
@EventLog(indexed=0)
public void NetworkProposalRegistered(String title, String description, int type, byte[] value, Address proposer) {}
```

## NetworkProposalCanceled

Triggered on any successful cancelProposal transaction.

```java
@EventLog(indexed=0)
public void NetworkProposalCanceled(byte[] id) {}
```

## NetworkProposalVoted

Triggered on any successful voteProposal transaction.

```java
@EventLog(indexed=0)
public void NetworkProposalVoted(byte[] id, int vote, Address voter) {}
```

## NetworkProposalApproved

Triggered on any successful voteProposal transaction approving network proposal.

```java
@EventLog(indexed=0)
public void NetworkProposalApproved(byte[] id) {}
```

## NetworkProposalDisapproved

Triggered on any successful transaction disapproving network proposal(voteProposal or BaseTransaction).

```java
@EventLog(indexed=0)
public void NetworkProposalDisapproved(byte[] id) {}
```

## NetworkProposalApplied

Triggered on any successful applyProposal transaction.

```java
@EventLog(indexed=0)
public void NetworkProposalApplied(byte[] id) {}
```

## NetworkProposalExpired

Triggered on successful Base transaction expiring network proposal.

```java
@EventLog(indexed=0)
public void NetworkProposalExpired(byte[] id) {}
```
