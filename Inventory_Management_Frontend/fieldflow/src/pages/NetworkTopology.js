import React, { useRef, useEffect, useState, useContext } from 'react';
import * as d3 from 'd3';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import Button from '@mui/material/Button';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import InputLabel from '@mui/material/InputLabel';
import FormControl from '@mui/material/FormControl';
import Paper from '@mui/material/Paper';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Close';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import { useTheme } from '@mui/material/styles';

// (icons are intentionally not rendered as React components inside SVG — SVG uses circles and labels)

import Header from '../components/Header';
import topologyService from '../services/topologyService';
import api from '../api/axiosInstance';
import { AuthContext } from '../context/AuthContext';

// Simple mapping for icons per node type (use text for now)
// default colors; will be replaced with theme-aware colors in component
const defaultNodeColors = {
  HEADEND: '#1976d2',
  CORE_SWITCH: '#9c27b0',
  FDH: '#ff9800',
  SPLITTER: '#4caf50',
  CUSTOMER: '#607d8b',
};

// Convert the simple single-child "path" format into children arrays for d3.hierarchy
function convertPathToHierarchy(node) {
  if (!node) return null;
  const out = {
    name: node.identifier || node.type || 'node',
    type: node.type || 'UNKNOWN',
    detail: node.detail,
    serialNumber: node.serialNumber,
    model: node.model,
    assets: node.assets || [],
    children: [],
  };
  if (node.child) {
    const c = convertPathToHierarchy(node.child);
    if (c) out.children.push(c);
  }
  // if there are customer assets, surface them as children nodes (ONT/ROUTER etc.)
  if (Array.isArray(node.assets) && node.assets.length) {
    const assetChildren = node.assets.map(a => ({
      name: a.assetType ? `${a.assetType} ${a.serialNumber || ''}`.trim() : (a.serialNumber || a.model || 'asset'),
      type: a.assetType || 'ASSET',
      detail: a.model || null,
      serialNumber: a.serialNumber || null,
      model: a.model || null,
      children: []
    }));
    out.children = out.children.concat(assetChildren);
  }
  return out;
}

// Convert HeadendTopologyDto (arrays) into a Hierarchical-like structure
function convertHeadendDto(dto) {
  if (!dto) return null;
  const root = {
    name: dto.name || `Headend-${dto.id}`,
    type: 'HEADEND',
    detail: dto.location,
    serialNumber: dto.serialNumber || null,
    model: dto.model || null,
    children: [],
  };

  (dto.coreSwitches || []).forEach(cs => {
    const csNode = {
      name: cs.name || `Core-${cs.id}`,
      type: 'CORE_SWITCH',
      detail: cs.location || cs.detail || null,
      serialNumber: cs.serialNumber || null,
      model: cs.model || null,
      children: [],
    };
    (cs.fdhs || []).forEach(f => {
      const fNode = {
        name: f.name || `FDH-${f.id}`,
        type: 'FDH',
        detail: f.region || f.location || null,
        serialNumber: f.serialNumber || null,
        model: f.model || null,
        children: [],
      };
      (f.splitters || []).forEach(sp => {
        const spNode = {
          name: sp.serialNumber || `Splitter-${sp.id}`,
          type: 'SPLITTER',
          // show capacity and FDH region/location in the detail
          detail: (sp.portCapacity || sp.capacity ? `${sp.portCapacity || sp.capacity} Ports` : null) + (f.region ? `, ${f.region}` : (f.location ? `, ${f.location}` : '')) || sp.model || null,
          serialNumber: sp.serialNumber || null,
          model: sp.model || null,
          children: [],
        };
        (sp.customers || []).forEach(cust => {
            const cNode = {
              name: cust.name || `Customer-${cust.customerId}`,
              type: 'CUSTOMER',
              detail: cust.status || null,
              serialNumber: null,
              model: null,
              children: [],
            };
            // surface assignedAssets as child nodes (ONT/ROUTER etc.)
            if (Array.isArray(cust.assignedAssets)) {
              cust.assignedAssets.forEach(a => {
                cNode.children.push({
                  name: a.assetType ? `${a.assetType} ${a.serialNumber || ''}`.trim() : (a.serialNumber || a.model || 'asset'),
                  type: a.assetType || 'ASSET',
                  detail: a.model || null,
                  serialNumber: a.serialNumber || null,
                  model: a.model || null,
                  children: []
                });
              });
            }
            spNode.children.push(cNode);
        });
        fNode.children.push(spNode);
      });
      csNode.children.push(fNode);
    });
    root.children.push(csNode);
  });

  return root;
}

const NetworkTopology = () => {
  const svgRef = useRef();
  const wrapperRef = useRef();
  const { user } = useContext(AuthContext);
  const theme = useTheme();
  const [mode, setMode] = useState('customer'); // customer | infrastructure | device | headend | fdh
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState([]);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [dataRoot, setDataRoot] = useState(null);
  const [loading, setLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogNode, setDialogNode] = useState(null);
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'info' });
  const canView = !!user;

  // theme-aware node colors
  const nodeColors = {
    HEADEND: theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main,
    CORE_SWITCH: theme.palette.mode === 'dark' ? theme.palette.secondary.light : theme.palette.secondary.main,
    FDH: theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.main,
    SPLITTER: theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main,
    CUSTOMER: theme.palette.mode === 'dark' ? theme.palette.info.light : theme.palette.info.main,
  };

  useEffect(() => {
    renderTree();
    // re-render on resize
    const ro = () => renderTree();
    window.addEventListener('resize', ro);
    return () => window.removeEventListener('resize', ro);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dataRoot, theme.palette.mode]);

  // load suggestion options when mode changes
  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setOptionsLoading(true);
      try {
        if (mode === 'customer') {
          const resp = await api.get('/api/customers/all');
          const opts = (resp.data || []).map(c => ({ label: `${c.id} — ${c.name}`, value: String(c.id), id: c.id, name: c.name }));
          if (!cancelled) setOptions(opts);
        } else if (mode === 'headend') {
          const resp = await api.get('/api/inventory/headends');
          const opts = (resp.data || []).map(h => ({ label: `${h.id} — ${h.name}`, value: String(h.id), id: h.id, name: h.name }));
          if (!cancelled) setOptions(opts);
        } else if (mode === 'fdh') {
          const resp = await api.get('/api/inventory/fdhs');
          const opts = (resp.data || []).map(f => ({ label: `${f.id} — ${f.name}`, value: String(f.id), id: f.id, name: f.name }));
          if (!cancelled) setOptions(opts);
        } else if (mode === 'infrastructure' || mode === 'device') {
          // both use serial numbers from assets
          const resp = await api.get('/api/inventory/assets');
          const items = resp.data || [];
          const seen = new Set();
          const opts = [];
          items.forEach(a => {
            const sn = a.serialNumber || a.serial || a.serial_number;
            if (sn && !seen.has(sn)) {
              seen.add(sn);
              opts.push({ label: sn, value: sn });
            }
          });
          if (!cancelled) setOptions(opts);
        } else {
          if (!cancelled) setOptions([]);
        }
      } catch (e) {
        // ignore option errors; keep options empty but show snack
        setSnack({ open: true, message: 'Failed to load suggestions', severity: 'warning' });
      } finally {
        if (!cancelled) setOptionsLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [mode]);

  const fetchTopology = async () => {
    if (!query) return;
    setLoading(true);
    // Normalize query for APIs that expect numeric id or pure serials.
    // If the user selected an Autocomplete option like "5 — Audrey Horne" we want to send only "5".
    let apiQuery = query;
    try {
      // try to resolve against options if present
      if (options && options.length) {
        const match = options.find(o => {
          if (!o) return false;
          if (typeof o === 'string') return o === query || o === (query || '').toString();
          return (o.label && o.label === query) || (o.value && o.value === query) || (o.id && String(o.id) === String(query));
        });
        if (match) {
          apiQuery = (typeof match === 'string') ? match : (match.value ?? match.id ?? match.label);
        } else if (typeof query === 'string' && query.includes('—')) {
          // fallback: split label like "5 — Name" and take the left side
          apiQuery = query.split('—')[0].trim();
        }
      } else if (typeof query === 'string' && query.includes('—')) {
        apiQuery = query.split('—')[0].trim();
      }

      let resp;
      if (mode === 'customer') {
        resp = await topologyService.getCustomerPath(apiQuery);
        // response contains path
        const path = resp.path;
        setDataRoot(convertPathToHierarchy(path));
      } else if (mode === 'infrastructure') {
        resp = await topologyService.getInfrastructurePath(apiQuery);
        setDataRoot(convertPathToHierarchy(resp.path));
      } else if (mode === 'device') {
        resp = await topologyService.getDevicePath(apiQuery);
        setDataRoot(convertPathToHierarchy(resp.path));
      } else if (mode === 'headend') {
        resp = await topologyService.getHeadendTopology(apiQuery);
        setDataRoot(convertHeadendDto(resp));
      } else if (mode === 'fdh') {
        resp = await topologyService.getFdhTopology(apiQuery);
        // backend may return different shapes; handle known variants
        // Example shapes:
        // { id,name,splitterViews: [...] }
        // { fdhId, fdhName, region, splitters: [...] }
        let root = null;
        if (resp) {
          if (resp.splitterViews) {
            root = {
              name: resp.name || `FDH-${resp.id}`,
              type: 'FDH',
              detail: resp.region || resp.location || null,
              serialNumber: resp.serialNumber || null,
              model: resp.model || null,
              children: (resp.splitterViews || []).map(sv => ({
                name: sv.serialNumber || (sv.splitterId ? String(sv.splitterId) : 'Splitter'),
                type: 'SPLITTER',
                // include port capacity and fdh region
                detail: (sv.portCapacity ? `${sv.portCapacity} Ports` : null) + (resp.region ? `, ${resp.region}` : '') || sv.model || null,
                serialNumber: sv.serialNumber || null,
                model: sv.model || null,
                children: (sv.customers || []).map(c => {
                  const custNode = { name: c.name || `Customer-${c.customerId}`, type: 'CUSTOMER', detail: c.status || null, children: [] };
                  (c.assignedAssets || []).forEach(a => {
                    custNode.children.push({ name: a.assetType ? `${a.assetType} ${a.serialNumber || ''}`.trim() : (a.serialNumber || a.model || 'asset'), type: a.assetType || 'ASSET', detail: a.model || null, serialNumber: a.serialNumber || null, model: a.model || null, children: [] });
                  });
                  return custNode;
                })
              }))
            };
          } else if (resp.splitters) {
            root = {
              name: resp.fdhName || resp.name || `FDH-${resp.fdhId || resp.id}`,
              type: 'FDH',
              detail: resp.region || resp.location || null,
              serialNumber: resp.serialNumber || null,
              model: resp.model || null,
              children: (resp.splitters || []).map(sp => ({
                name: sp.serialNumber || (sp.splitterId ? String(sp.splitterId) : (sp.id ? String(sp.id) : 'Splitter')),
                type: 'SPLITTER',
                detail: (sp.capacity || sp.portCapacity ? `${sp.capacity || sp.portCapacity} Ports` : null) + (resp.region ? `, ${resp.region}` : '') || sp.model || null,
                serialNumber: sp.serialNumber || null,
                model: sp.model || null,
                children: (sp.connectedCustomers || []).map(c => {
                  const custNode = {
                    name: c.name || `Customer-${c.customerId}`,
                    type: 'CUSTOMER',
                    detail: c.status || null,
                    serialNumber: null,
                    model: null,
                    children: []
                  };
                  (c.assignedAssets || []).forEach(a => {
                    custNode.children.push({
                      name: a.assetType ? `${a.assetType} ${a.serialNumber || ''}`.trim() : (a.serialNumber || a.model || 'asset'),
                      type: a.assetType || 'ASSET',
                      detail: a.model || null,
                      serialNumber: a.serialNumber || null,
                      model: a.model || null,
                      children: []
                    });
                  });
                  return custNode;
                })
              }))
            };
          } else {
            // fallback: try to map generically
            root = {
              name: resp.name || resp.fdhName || `FDH-${resp.fdhId || resp.id}`,
              type: 'FDH',
              detail: resp.region || resp.location || null,
              serialNumber: resp.serialNumber || null,
              model: resp.model || null,
              children: []
            };
          }
        }
        setDataRoot(root);
      }
    } catch (e) {
      const msg = e?.response?.data?.message || e.message || 'Failed to load topology';
      setSnack({ open: true, message: msg, severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  function renderTree() {
    const container = wrapperRef.current;
    const svgEl = svgRef.current;
    if (!svgEl || !container) return;

    // clear
    d3.select(svgEl).selectAll('*').remove();

    const width = container.clientWidth || 1000;
    const height = Math.max(400, (container.clientHeight || 600));

    if (!dataRoot) {
      // show placeholder text
      const placeholder = d3.select(svgEl).append('g');
      placeholder.append('text').attr('x', 20).attr('y', 20).text('No topology loaded. Use the controls above to load a path or headend.');
      return;
    }

    // Create hierarchy
    const root = d3.hierarchy(dataRoot, d => d.children && d.children.length ? d.children : null);

    // layout: horizontal tree
    const treeLayout = d3.tree().nodeSize([120, 160]);
    treeLayout(root);

    // compute bounds
    const nodes = root.descendants();
    const links = root.links();

    // compute extents for y to map into height
    const minX = d3.min(nodes, d => d.x);
    const maxX = d3.max(nodes, d => d.x);
    const minY = d3.min(nodes, d => d.y);
    const maxY = d3.max(nodes, d => d.y);

    const xScale = d3.scaleLinear().domain([minX, maxX]).range([40, height - 40]);
    const yScale = d3.scaleLinear().domain([minY, maxY]).range([40, width - 40]);

    const svgSel = d3.select(svgEl)
      .attr('width', width)
      .attr('height', Math.max(500, height));

    const g = svgSel.append('g').attr('transform', `translate(0,0)`);

    // zoom
    const zoom = d3.zoom().on('zoom', (event) => {
      g.attr('transform', event.transform);
    });
    svgSel.call(zoom);

    // links
    g.selectAll('path.link')
      .data(links)
      .enter()
      .append('path')
      .attr('class', 'link')
      .attr('fill', 'none')
      .attr('stroke', '#999')
      .attr('stroke-width', 1.5)
      .attr('d', d => {
        const sx = yScale(d.source.y);
        const sy = xScale(d.source.x);
        const tx = yScale(d.target.y);
        const ty = xScale(d.target.x);
        return `M${sx},${sy}C${(sx + tx) / 2},${sy} ${(sx + tx) / 2},${ty} ${tx},${ty}`;
      });

    // nodes
    const node = g.selectAll('g.node')
      .data(nodes)
      .enter()
      .append('g')
      .attr('class', 'node')
      .attr('transform', d => `translate(${yScale(d.y)},${xScale(d.x)})`)
      .style('cursor', 'pointer')
      .on('click', (event, d) => {
        setDialogNode(d.data);
        setDialogOpen(true);
      });

    // Render nodes as boxes with labels inside
    const boxWidth = 180;
    const boxHeight = 44;

    node.append('rect')
      .attr('x', -boxWidth / 2)
      .attr('y', -boxHeight / 2)
      .attr('width', boxWidth)
      .attr('height', boxHeight)
      .attr('rx', 6)
      .attr('fill', d => nodeColors[d.data.type] || defaultNodeColors[d.data.type] || '#777')
      .attr('stroke', theme.palette.divider)
      .attr('stroke-width', 1);

    // main label (name and optional port/status)
    node.append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', -4)
      .style('font-size', 12)
      .style('fill', theme.palette.getContrastText(theme.palette.background.paper))
      .text(d => {
        if (d.data.type === 'CUSTOMER') {
          // detail likely contains port/status string constructed earlier
          return `${d.data.name}`;
        }
        if (d.data.type === 'SPLITTER') {
          return d.data.name || 'Splitter';
        }
        return d.data.name || d.data.type || 'node';
      });

    // secondary line (type or port/status)
    node.append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', 12)
      .style('font-size', 10)
      .attr('fill', theme.palette.text.secondary)
      .text(d => {
        if (d.data.type === 'CUSTOMER') {
          return d.data.detail || '';
        }
        // hide splitter port/capacity info per user request
        if (d.data.type === 'SPLITTER') {
          return '';
        }
        return d.data.type || '';
      });

    // no extra badges for splitters (user requested to hide port counts)

    // double-click to toggle collapse/expand
    node.on('dblclick', (event, d) => {
      // toggle children in the source data
      const src = d.data;
      if (src.children && src.children.length) {
        src._children = src.children;
        src.children = [];
      } else if (src._children) {
        src.children = src._children;
        src._children = null;
      }
      renderTree();
    });

    // fit / center initially
    // fit / center placeholder (bbox used below)

    // compute bounding box and auto-fit
    setTimeout(() => {
      try {
        const bbox = g.node().getBBox();
        const scale = Math.min((width - 80) / bbox.width, (height - 80) / bbox.height, 1);
        const tx = (width / 2) - (bbox.x + bbox.width / 2) * scale;
        const ty = (height / 2) - (bbox.y + bbox.height / 2) * scale;
        const transform = d3.zoomIdentity.translate(tx, ty).scale(scale);
        svgSel.transition().duration(400).call(zoom.transform, transform);
      } catch (e) {
        // ignore
      }
    }, 50);

    // (mini-map removed)
  }
  // mini-map removed

  // mini-map removed

  if (!canView) return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
        <Typography variant="h6">Please login to view topology</Typography>
      </Container>
    </>
  );

  return (
    <>
      <Header />
      <Container sx={{ pt: 12, pb: 6 }}>
        <Typography variant="h4" sx={{ mb: 2 }}>Network Topology</Typography>

        <Paper sx={{ p: 2, mb: 2 }}>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel id="topology-mode">Mode</InputLabel>
              <Select labelId="topology-mode" value={mode} label="Mode" onChange={(e) => setMode(e.target.value)}>
                <MenuItem value="customer">Customer Path (customer/{'{id}'})</MenuItem>
                <MenuItem value="infrastructure">Infrastructure (serial)</MenuItem>
                <MenuItem value="device">Device (serial)</MenuItem>
                <MenuItem value="headend">Headend (full)</MenuItem>
                <MenuItem value="fdh">FDH (focused)</MenuItem>
              </Select>
            </FormControl>

            <Autocomplete
              freeSolo
              size="small"
              options={options}
              loading={optionsLoading}
              getOptionLabel={(opt) => (typeof opt === 'string' ? opt : (opt.label || opt.value || ''))}
              inputValue={query}
              onInputChange={(e, val) => setQuery(val)}
              onChange={(e, val) => {
                if (!val) return;
                // val may be an object {value} or a string
                if (typeof val === 'string') setQuery(val);
                else setQuery(val.value ?? val.label ?? '');
              }}
              sx={{ minWidth: 320 }}
              renderInput={(params) => <TextField {...params} label="ID / Serial" />}
            />
            <Button variant="contained" onClick={fetchTopology} disabled={loading}>{loading ? 'Loading...' : 'Load'}</Button>
            <Button variant="outlined" onClick={() => { setDataRoot(null); setQuery(''); }}>Clear</Button>
          </Box>
        </Paper>

        <Paper ref={wrapperRef} sx={{ height: '60vh', p: 0, overflow: 'hidden' }}>
          <svg ref={svgRef} style={{ width: '100%', height: '100%' }} />
        </Paper>

        <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
          <DialogTitle>
            Node details
            <IconButton aria-label="close" onClick={() => setDialogOpen(false)} sx={{ position: 'absolute', right: 8, top: 8 }}>
              <CloseIcon />
            </IconButton>
          </DialogTitle>
          <DialogContent>
            {dialogNode && (
              <Box>
                <Typography variant="h6">{dialogNode.name} <small style={{ color: '#666' }}>({dialogNode.type})</small></Typography>
                {dialogNode.detail && <Typography><strong>Detail:</strong> {dialogNode.detail}</Typography>}
                {dialogNode.serialNumber && <Typography><strong>Serial:</strong> {dialogNode.serialNumber}</Typography>}
                {dialogNode.model && <Typography><strong>Model:</strong> {dialogNode.model}</Typography>}
                {(() => {
                  const assets = (dialogNode.assets && dialogNode.assets.length) ? dialogNode.assets : (dialogNode.children ? dialogNode.children.filter(c => ['ONT','ROUTER','ASSET'].includes(c.type)) : []);
                  if (assets.length === 0) return null;
                  return (
                    <Box sx={{ mt: 1 }}>
                      <Typography variant="subtitle2">Customer Assets</Typography>
                      {assets.map((a, i) => (
                        <Paper key={i} sx={{ p: 1, mt: 1 }}>
                          <Typography>{a.assetType || a.type} — {a.serialNumber || a.name} {a.model ? `(${a.model})` : ''}</Typography>
                        </Paper>
                      ))}
                    </Box>
                  );
                })()}
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>

        <Snackbar open={snack.open} autoHideDuration={5000} onClose={() => setSnack(s => ({ ...s, open: false }))} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.message}</Alert>
        </Snackbar>
      </Container>
    </>
  );
};

export default NetworkTopology;
