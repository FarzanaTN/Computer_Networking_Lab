class Router:
    def __init__(self, router_id):
        self.id = router_id             # Unique Router ID (e.g., A, B)
        
        self.neighbors = {}             # Map<String, Integer> – Direct neighbors and cost
        
        self.routingTable = {}          # Map<String, (cost, nextHop)>
        
        self.distanceVector = {}        # Map<String, Integer> – Current known costs

    
    def updateRoutingTable(self, new_table):
        self.routingTable = new_table
        self.distanceVector = {dest: cost for dest, (cost, _) in new_table.items()}
