function DataTable({ data, columns, rows, empty = "No items yet." }) {
  if (data.loading) return <p role="status">Loading…</p>;
  if (!data.data) return null;
  return data.data.content.length ? (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c}>{c}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.data.content.map((item) => (
            <tr key={item.id}>
              {rows(item).map((cell, i) => (
                <td key={i}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  ) : (
    <p className="empty">{empty}</p>
  );
}
export default DataTable;
