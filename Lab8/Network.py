# class Network:
#     def __init__(self, topology_file):
#         self.routers = {}                   # Map<String, Router>
#         self.topologyFile = topology_file   #String – Input file name (e.g., topology.txt)
#         self.messageCount = 0               #Integer – Total messages exchanged
#         self.log = []                       #List<String> – Records of all routing changes

#     # Read the file and build the graph by:
#     # -Adding each router as a node
#     # - Populating neighbors and their link costs
#     # - Initializing routing tables (cost to self = 0; neighbors = cost; others = ∞)
      
#     def readTopology(fileName) :
        
    
#     # For each router, populate: routingT able[router][destination] = (cost, nextHop)
#     def initializeRoutingTables() :
        
#     # Each router sends its vector to all neighbors.
#     def sendDistanceVector() :
        
    
    
#     # Perform Bellman-Ford: Dx(y) = min (Dx(y), c(x, v) + Dv(y)) , Update routing table if better path is found.
#     def receiveVector(routerId, fromNeighbor, vector) :
        
#     # If a route to destination D uses neighbor N as next hop, advertise cost = ∞ to N for D.
#     def applyPoisonReverse() :
        
#     # Every 30s, pick a random edge and update its cost (increase or decrease). Propagate changes to neighbors.
#     def updateCostRandomly() : 
        
        
#     # Print current routing table for a router.
#     def printRoutingTable(routerId) :
        
#     # Append change information to system logs.  
#     def logChange(message) :
        
        
        
        
    
        
        
             
                
# network.py
import random
import threading
import time
from Router import Router

INF = float('inf')

class Network:
    def __init__(self, topology_file):
        self.routers = {}        # {router_id: Router}
        self.topologyFile = topology_file
        self.messageCount = 0
        self.log = []
        
    # self.readTopology(start_time)
        
    
    def printRouters(self):
        print("\n=== Routers in the Network ===")
        for router_id in sorted(self.routers.keys()):
            print(f"- Router {router_id}")


    # def readTopology(self):
    #     with open(self.topologyFile, 'r') as file:
    #         for line in file:
    #             r1, r2, cost = line.strip().split()
    #             cost = int(cost)
    #             for router_id in (r1, r2):
    #                 if router_id not in self.routers:
    #                     self.routers[router_id] = Router(router_id)
    #             self.routers[r1].neighbors[r2] = cost
    #             self.routers[r2].neighbors[r1] = cost
        
    #     self.printRouters()
    #     self.initializeRoutingTables()

    # def initializeRoutingTables(self):
    #     for router in self.routers.values():
    #         for dest_id in self.routers:
    #             if dest_id == router.id:
    #                 router.routingTable[dest_id] = (0, router.id)
    #             elif dest_id in router.neighbors:
    #                 router.routingTable[dest_id] = (router.neighbors[dest_id], dest_id)
    #             else:
    #                 router.routingTable[dest_id] = (INF, None)
    #         router.updateRoutingTable(router.routingTable.copy())
    #         self.printRoutingTable(router.id)

    def readTopology(self, start_time):
        with open(self.topologyFile, 'r') as file:
            for line in file:
                r1, r2, cost = line.strip().split()
                cost = int(cost)
                for router_id in (r1, r2):
                    if router_id not in self.routers:
                        self.routers[router_id] = Router(router_id)
                self.routers[r1].neighbors[r2] = cost
                self.routers[r2].neighbors[r1] = cost
        self.initializeRoutingTables(start_time)


    def initializeRoutingTables(self, start_time):
        for router in self.routers.values():
            for dest_id in self.routers:
                if dest_id == router.id:
                    router.routingTable[dest_id] = (0, router.id)
                elif dest_id in router.neighbors:
                    router.routingTable[dest_id] = (router.neighbors[dest_id], dest_id)
                else:
                    router.routingTable[dest_id] = (float('inf'), None)
            router.updateRoutingTable(router.routingTable.copy())
            self.printRoutingTable(router.id, start_time, initial=True)

    
    #  def sendDistanceVector(self, start_time):
    #     for router_id, router in self.routers.items():
    #         vector = router.distanceVector.copy()
    #         for neighbor_id in router.neighbors:
    #             self.receiveVector(neighbor_id, router_id, vector)


    def sendDistanceVector(self, start_time):
        for router_id, router in self.routers.items():
            vector = router.distanceVector.copy()
            for neighbor_id in router.neighbors:
                self.receiveVector(neighbor_id, router_id, vector, start_time)


    def receiveVector(self, to_router_id, from_router_id, vector, start_time):
        to_router = self.routers[to_router_id]
        updated = False
        for dest in vector:
            if dest == to_router_id:
                continue
            cost_to_neighbor = to_router.neighbors[from_router_id]
            new_cost = cost_to_neighbor + vector[dest]
            old_cost, old_next = to_router.routingTable[dest]
            if new_cost < old_cost:
                to_router.routingTable[dest] = (new_cost, from_router_id)
                updated = True
        if updated:
            to_router.updateRoutingTable(to_router.routingTable.copy())
            self.messageCount += 1
            self.logChange(f"Routing table updated at Router {to_router_id}")
            self.printRoutingTable(to_router_id, start_time)
            #self.printRoutingTable(to_router_id, start_time)

    def applyPoisonReverse(self):
        for router in self.routers.values():
            for neighbor_id in router.neighbors:
                poisoned_vector = router.distanceVector.copy()
                for dest in poisoned_vector:
                    _, next_hop = router.routingTable[dest]
                    if next_hop == neighbor_id:
                        poisoned_vector[dest] = INF
                self.receiveVector(neighbor_id, router.id, poisoned_vector)

    # def updateCostRandomly(self):
    #     all_links = []
    #     for r1 in self.routers:
    #         for r2 in self.routers[r1].neighbors:
    #             if (r2, r1) not in all_links:
    #                 all_links.append((r1, r2))

    #     r1, r2 = random.choice(all_links)
    #     new_cost = random.randint(1, 20)
    #     self.routers[r1].neighbors[r2] = new_cost
    #     self.routers[r2].neighbors[r1] = new_cost
    #     self.logChange(f"Cost updated between {r1} and {r2} to {new_cost}")
    #     self.initializeRoutingTables()

    def updateCostRandomly(self, start_time):
        all_links = []
        for r1 in self.routers:
            for r2 in self.routers[r1].neighbors:
                if (r2, r1) not in all_links:
                    all_links.append((r1, r2))
        r1, r2 = random.choice(all_links)
        old_cost = self.routers[r1].neighbors[r2]
        new_cost = random.randint(1, 20)
        self.routers[r1].neighbors[r2] = new_cost
        self.routers[r2].neighbors[r1] = new_cost
        elapsed = int(time.time() - start_time)
        print(f"[Time = {elapsed}s] Cost updated: {r1} <-> {r2} changed from {old_cost} to {new_cost}")
        self.initializeRoutingTables(start_time)


    # def printRoutingTable(self, router_id):
    #     import time
    #     rt = self.routers[router_id].routingTable
    #     timestamp = int(time.time() % 1000)  # You can format this differently if needed
    #     print(f"\n[Router {router_id}] Table Update at Time = {timestamp}s")
    #     print("Dest | Cost | Next Hop")
    #     print("-----------------------")
    #     for dest in sorted(rt):
    #         cost, next_hop = rt[dest]
    #         cost_str = "∞" if cost == float('inf') else str(cost)
    #         next_hop_str = next_hop if next_hop is not None else "-"
    #         print(f"{dest:<5} | {cost_str:<5} | {next_hop_str}")
    
    
    def printRoutingTable(self, router_id, start_time, initial=False):
        router = self.routers[router_id]
        elapsed = int(time.time() - start_time)
        label = "Initial Routing Table" if initial else "Routing Table"
        print(f"[Time = {elapsed}s] {label} at Router {router_id}:")
        print("Dest | Cost | Next Hop")
        print("-----------------------")
        for dest in sorted(router.routingTable):
            cost, next_hop = router.routingTable[dest]
            cost_str = "∞" if cost == float('inf') else str(cost)
            print(f"{dest} | {cost_str} | {next_hop}")
        print()


    def logChange(self, message):
        self.log.append(message)
        print("[LOG]", message)

    def run(self):
        start_time = time.time()
        self.readTopology(start_time)  
        iteration = 0
        converged = False
        previous_states = {}
        while not converged and iteration < 100:
            state_changed = False
            self.sendDistanceVector(start_time)
            
            current_states = {
                rid: r.distanceVector.copy() 
                for rid, r in self.routers.items()
            }
            
            if current_states != previous_states:
                previous_states = current_states
                state_changed = True
                
            if not state_changed:
                converged = True
                
            iteration += 1
            time.sleep(5)
            
            if iteration % 6 == 0:  # Every 30 seconds
                self.updateCostRandomly()

        print("\n===== FINAL OUTPUT =====")
        for router_id in self.routers:
            self.printRoutingTable(router_id, start_time)
        # print(f"Total messages exchanged: {self.messageCount}")
        # print(f"Total convergence iterations: {iteration}")
        elapsed = int(time.time() - start_time)
        print(f"[Time = {elapsed}s] Convergence complete. Total messages exchanged: {self.messageCount}")
