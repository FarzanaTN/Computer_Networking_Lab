import pandas as pd
import matplotlib.pyplot as plt

# Load CSV files
tahoe_df = pd.read_csv("log_tahoe.csv")
reno_df = pd.read_csv("log_reno.csv")

# Plot both
plt.figure(figsize=(10, 6))

plt.plot(tahoe_df["Round"], tahoe_df["cwnd"], label="TCP Tahoe", marker='o', color='blue')
plt.plot(reno_df["Round"], reno_df["cwnd"], label="TCP Reno", marker='s', color='green')

# Add labels and title
plt.title("TCP Tahoe vs TCP Reno - Congestion Window (cwnd) Over Time")
plt.xlabel("Round")
plt.ylabel("Congestion Window Size (cwnd)")
plt.legend()
plt.grid(True)

# Save the plot to a file
plt.savefig("tcp_tahoe_vs_reno.png")
plt.show()
