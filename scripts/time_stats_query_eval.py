import sys

def read_data(file_path):
    """Reads numeric values from the second column of a file."""
    values = []

    try:
        with open(file_path, 'r') as file:
            for line in file:
                parts = line.strip().split()  # Splitting by whitespace
                print(parts)

                if len(parts) >= 2:  # Ensuring at least two columns exist
                    try:
                        values.append(float(parts[1].replace(',','.')))  # Extracting the number
                    except ValueError:
                        print(f"Skipping invalid line: {line.strip()}")
    
        return values

    except FileNotFoundError:
        print("Error: File not found.")
        return []

def compute_statistics(values):
    """Computes the average, min, and max of a list of numbers."""
    if not values:
        return None, None, None  # Return None if list is empty

    average = sum(values) / len(values)
    minimum = min(values)
    maximum = max(values)

    return average, minimum, maximum

def main():
    """Main function to handle user input and display results."""
    if len(sys.argv) != 2:
        print("Usage: python script.py <file_path>")
        return
    
    file_path = sys.argv[1]
    values = read_data(file_path)
    average, minimum, maximum = compute_statistics(values)

    if values:
        print(f"Average: {average:.6f}")
        print(f"Min: {minimum:.6f}")
        print(f"Max: {maximum:.6f}")
    else:
        print("No valid data found in the file.")

if __name__ == "__main__":
    main()
